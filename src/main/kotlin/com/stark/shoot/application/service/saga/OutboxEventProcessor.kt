package com.stark.shoot.application.service.saga

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxEventEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.domain.saga.SagaState
import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Outbox 이벤트 프로세서
 *
 * 주기적으로 Outbox 테이블을 스캔하여 처리되지 않은 이벤트를 발행합니다.
 * 이를 통해 이벤트 발행이 보장됩니다.
 */
@Service
class OutboxEventProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val eventPublisher: EventPublishPort,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        const val MAX_RETRY_COUNT = 5
        const val OUTBOX_RETENTION_DAYS = 7L
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
            // 재시도 횟수 체크
            if (outboxEvent.retryCount >= MAX_RETRY_COUNT) {
                logger.error { "Max retry count exceeded for event: id=${outboxEvent.id}, sagaId=${outboxEvent.sagaId}" }
                outboxEvent.updateSagaState(SagaState.FAILED)
                outboxEventRepository.save(outboxEvent)
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
