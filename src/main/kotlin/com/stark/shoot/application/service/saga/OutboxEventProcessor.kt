package com.stark.shoot.application.service.saga

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxDeadLetterEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxEventEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxDeadLetterRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.notification.SlackNotificationPort
import com.stark.shoot.domain.saga.SagaState
import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionSynchronization
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Outbox 이벤트 프로세서
 *
 * 주기적으로 Outbox 테이블을 스캔하여 처리되지 않은 이벤트를 발행합니다.
 * 이를 통해 이벤트 발행이 보장됩니다.
 *
 * **CDC (Change Data Capture)와의 관계:**
 * - CDC 활성화 시: 이 프로세서는 **백업 역할**로 동작
 *   - CDC가 WAL에서 실시간으로 이벤트 발행 (<100ms)
 *   - 이 프로세서는 CDC가 놓친 이벤트만 발행 (5초 주기)
 * - CDC 비활성화 시: 이 프로세서가 **주 발행자** 역할
 *   - 5초마다 폴링하여 이벤트 발행
 *
 * **동작 방식:**
 * 1. CDC가 이벤트를 처리하면 processed=true로 업데이트
 * 2. 이 프로세서는 processed=false인 이벤트만 조회
 * 3. 결과적으로 CDC가 정상이면 조회 결과가 없음 (백업 대기)
 * 4. CDC 장애 시 자동으로 백업 발행 시작
 *
 * **Dead Letter Queue (DLQ):**
 * - 재시도 5회 초과 시 DLQ로 이동
 * - 운영자 수동 확인 및 재처리 가능
 * - Slack 알림으로 즉시 문제 인지
 */
@Service
class OutboxEventProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val deadLetterRepository: OutboxDeadLetterRepository,
    private val eventPublisher: EventPublishPort,
    private val slackNotificationPort: SlackNotificationPort,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        const val MAX_RETRY_COUNT = 5
        const val OUTBOX_RETENTION_DAYS = 7L
        const val DLQ_RETENTION_DAYS = 30L  // DLQ는 30일 보관
    }

    /**
     * 처리되지 않은 Outbox 이벤트 발행
     * 매 5초마다 실행
     *
     * @SchedulerLock: 분산 환경에서 중복 실행 방지
     * - lockAtMostFor: 9분 (작업이 실패해도 이 시간 후 락 해제)
     * - lockAtLeastFor: 1초 (최소 대기 시간, 너무 빠른 재실행 방지)
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @SchedulerLock(name = "processOutboxEvents", lockAtMostFor = "9m", lockAtLeastFor = "1s")
    @Transactional
    fun processOutboxEvents() {
        try {
            val unprocessedEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()

            if (unprocessedEvents.isEmpty()) {
                logger.debug { "No outbox events to process" }
                return
            }

            logger.info { "Processing ${unprocessedEvents.size} outbox events" }

            unprocessedEvents.forEach { event ->
                processEvent(event)
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to process outbox events" }
        }
    }

    /**
     * 개별 이벤트 처리
     */
    private fun processEvent(outboxEvent: OutboxEventEntity) {
        try {
            // 재시도 횟수 체크 - 초과 시 DLQ로 이동
            if (outboxEvent.retryCount >= MAX_RETRY_COUNT) {
                moveToDLQ(outboxEvent)
                return
            }

            // 이벤트 역직렬화
            val eventClass = Class.forName(outboxEvent.eventType)
            val event = objectMapper.readValue(outboxEvent.payload, eventClass) as com.stark.shoot.domain.event.DomainEvent

            // 이벤트 발행
            eventPublisher.publishEvent(event)

            // 성공 처리
            outboxEvent.markAsProcessed()
            outboxEventRepository.save(outboxEvent)

            logger.debug { "Outbox event processed: id=${outboxEvent.id}, type=${outboxEvent.eventType}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to process outbox event: id=${outboxEvent.id}" }

            // 재시도 증가
            outboxEvent.incrementRetry(e.message ?: "Unknown error")
            outboxEventRepository.save(outboxEvent)
        }
    }

    /**
     * DLQ로 이벤트 이동
     *
     * 재시도 횟수를 초과한 이벤트를 Dead Letter Queue로 이동합니다.
     */
    private fun moveToDLQ(outboxEvent: OutboxEventEntity) {
        try {
            val eventId = outboxEvent.id
                ?: throw IllegalStateException("Cannot move event to DLQ: event ID is null")

            val dlqEvent = OutboxDeadLetterEntity(
                originalEventId = eventId,
                sagaId = outboxEvent.sagaId,
                sagaState = outboxEvent.sagaState,
                eventType = outboxEvent.eventType,
                payload = outboxEvent.payload,
                failureReason = outboxEvent.lastError ?: "재시도 횟수 초과 (${MAX_RETRY_COUNT}회)",
                failureCount = outboxEvent.retryCount,
                lastFailureAt = Instant.now()
            )

            // DLQ에 저장
            deadLetterRepository.save(dlqEvent)

            // 원본 이벤트는 처리 완료로 표시 (더 이상 재시도 안 함)
            outboxEvent.markAsProcessed()
            outboxEvent.updateSagaState(SagaState.FAILED)
            outboxEventRepository.save(outboxEvent)

            logger.error {
                "이벤트를 DLQ로 이동: " +
                "outboxId=${outboxEvent.id}, " +
                "sagaId=${outboxEvent.sagaId}, " +
                "eventType=${outboxEvent.eventType}, " +
                "reason=${dlqEvent.failureReason}"
            }

            // Slack 알림 전송 - 트랜잭션 커밋 후 실행
            // 트랜잭션이 롤백되면 알림도 전송되지 않음
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        slackNotificationPort.notifyDLQEvent(
                            sagaId = outboxEvent.sagaId,
                            eventType = outboxEvent.eventType,
                            failureReason = dlqEvent.failureReason
                        )
                    }
                }
            )

        } catch (e: Exception) {
            logger.error(e) { "DLQ 이동 실패: outboxId=${outboxEvent.id}" }
        }
    }

    /**
     * 오래된 처리 완료 이벤트 정리
     * 매일 자정 실행
     *
     * @SchedulerLock: 한 인스턴스만 정리 작업 수행
     * - lockAtMostFor: 10분
     * - lockAtLeastFor: 1초
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "cleanupOldEvents", lockAtMostFor = "10m", lockAtLeastFor = "1s")
    @Transactional
    fun cleanupOldEvents() {
        try {
            val threshold = Instant.now().minus(OUTBOX_RETENTION_DAYS, ChronoUnit.DAYS)
            val oldEvents = outboxEventRepository.findOldProcessedEvents(threshold)

            if (oldEvents.isNotEmpty()) {
                outboxEventRepository.deleteAll(oldEvents)
                logger.info { "Cleaned up ${oldEvents.size} old outbox events" }
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup old outbox events" }
        }
    }

    /**
     * 오래된 DLQ 이벤트 정리
     * 매일 자정 1시 실행
     *
     * 30일 이상 지난 해결된 DLQ를 삭제합니다.
     *
     * @SchedulerLock: 한 인스턴스만 정리 작업 수행
     */
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "cleanupOldDLQ", lockAtMostFor = "10m", lockAtLeastFor = "1s")
    @Transactional
    fun cleanupOldDLQ() {
        try {
            val threshold = Instant.now().minus(DLQ_RETENTION_DAYS, ChronoUnit.DAYS)
            val oldDLQEvents = deadLetterRepository.findOldResolvedDLQ(threshold)

            if (oldDLQEvents.isNotEmpty()) {
                deadLetterRepository.deleteAll(oldDLQEvents)
                logger.info { "DLQ 정리 완료: ${oldDLQEvents.size}개 삭제" }
            }

        } catch (e: Exception) {
            logger.error(e) { "DLQ 정리 실패" }
        }
    }

    /**
     * 미해결 DLQ 모니터링
     * 매 시간마다 실행
     *
     * 미해결 DLQ가 있으면 로그와 알림을 전송합니다.
     *
     * @SchedulerLock: 한 인스턴스만 모니터링 수행
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "monitorUnresolvedDLQ", lockAtMostFor = "5m", lockAtLeastFor = "1s")
    @Transactional(readOnly = true)
    fun monitorUnresolvedDLQ() {
        try {
            val unresolvedCount = deadLetterRepository.countByResolvedFalse()

            if (unresolvedCount > 0) {
                val recentDLQ = deadLetterRepository.findTop10ByResolvedFalseOrderByCreatedAtDesc()

                val recentDLQInfo = recentDLQ.joinToString("\n") {
                    "id=${it.id}, sagaId=${it.sagaId}, eventType=${it.eventType}, reason=${it.failureReason}"
                }

                logger.error {
                    "미해결 DLQ 발견: 총 ${unresolvedCount}개\n" +
                    "최근 10개:\n" +
                    recentDLQ.joinToString("\n") {
                        "  - id=${it.id}, sagaId=${it.sagaId}, " +
                        "eventType=${it.eventType}, " +
                        "reason=${it.failureReason}, " +
                        "createdAt=${it.createdAt}"
                    }
                }

                // Slack 알림 전송 - 트랜잭션 커밋 후 실행
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() {
                            slackNotificationPort.notifyUnresolvedDLQ(
                                unresolvedCount = unresolvedCount,
                                recentDLQInfo = recentDLQInfo
                            )
                        }
                    }
                )
            }

        } catch (e: Exception) {
            logger.error(e) { "DLQ 모니터링 실패" }
        }
    }

    /**
     * 재시도 한계를 초과한 이벤트 모니터링
     * 매 시간마다 실행
     *
     * @SchedulerLock: 한 인스턴스만 모니터링 수행
     * - lockAtMostFor: 5분
     * - lockAtLeastFor: 1초
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "monitorFailedEvents", lockAtMostFor = "5m", lockAtLeastFor = "1s")
    @Transactional(readOnly = true)
    fun monitorFailedEvents() {
        try {
            val failedEvents = outboxEventRepository.findFailedEventsExceedingRetries(MAX_RETRY_COUNT)

            if (failedEvents.isNotEmpty()) {
                logger.error {
                    "Found ${failedEvents.size} failed events requiring manual intervention:\n" +
                    failedEvents.joinToString("\n") {
                        "  - id=${it.id}, sagaId=${it.sagaId}, type=${it.eventType}, error=${it.lastError}"
                    }
                }
                // TODO: 알림 전송 (Slack, 이메일 등)
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to monitor failed events" }
        }
    }
}
