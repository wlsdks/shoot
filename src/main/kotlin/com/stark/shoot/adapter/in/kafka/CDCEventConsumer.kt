package com.stark.shoot.adapter.`in`.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.out.event.EventPublishPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * CDC 이벤트 소비자
 *
 * Debezium이 Outbox 테이블에서 감지한 변경사항을 Kafka에서 소비합니다.
 * - Topic: shoot.events.* (Outbox Event Router가 이벤트 타입별로 라우팅)
 * - Debezium의 Outbox Pattern 구현
 *
 * **동작 방식**:
 * 1. Debezium이 PostgreSQL WAL에서 Outbox 테이블 변경 감지
 * 2. Outbox Event Router Transform이 이벤트 타입별로 토픽 분리
 * 3. 이 Consumer가 토픽에서 이벤트 소비
 * 4. 실제 비즈니스 이벤트를 내부 Kafka 토픽으로 재발행
 *
 * **OutboxEventProcessor와의 관계**:
 * - CDC가 정상: CDC가 실시간 발행 (<100ms)
 * - CDC 장애 시: OutboxEventProcessor가 폴링으로 백업 발행 (5초 주기)
 */
@Component
class CDCEventConsumer(
    private val eventPublisher: EventPublishPort,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * CDC 이벤트 소비
     *
     * Debezium Outbox Pattern:
     * - Topic: shoot.events.{EventType}
     * - Key: saga_id
     * - Value: payload (JSON)
     * - Headers: sagaId, eventType
     *
     * @param payload 이벤트 페이로드 (JSON)
     * @param sagaId Saga ID (Header)
     * @param eventType 이벤트 타입 (Header)
     * @param topic Kafka 토픽
     * @param partition 파티션 번호
     * @param offset 오프셋
     */
    @KafkaListener(
        topicPattern = "shoot\\.events\\..*",  // shoot.events.로 시작하는 모든 토픽
        groupId = "shoot-cdc-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun consumeCDCEvent(
        @Payload payload: String,
        @Header(value = "sagaId", required = false) sagaId: String?,
        @Header(value = "eventType", required = false) eventType: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        logger.info {
            "CDC 이벤트 수신: topic=$topic, partition=$partition, offset=$offset, " +
            "sagaId=$sagaId, eventType=$eventType"
        }

        try {
            // 1. 이벤트 타입 결정
            val actualEventType = eventType ?: extractEventTypeFromTopic(topic)

            // 2. 이벤트 역직렬화
            val eventClass = Class.forName(actualEventType)
            val event = objectMapper.readValue(payload, eventClass) as com.stark.shoot.domain.event.DomainEvent

            // 3. 실제 비즈니스 이벤트 발행
            // Kafka의 chat-messages, chat-notifications 등의 토픽으로 재발행
            eventPublisher.publishEvent(event)

            logger.info {
                "CDC 이벤트 처리 완료: eventType=$actualEventType, sagaId=$sagaId"
            }

            // 4. Outbox 테이블 업데이트 (processed=true)
            // CDC가 처리했음을 표시하여 OutboxEventProcessor가 스킵하도록 함
            if (sagaId != null) {
                markAsProcessedBySagaId(sagaId, actualEventType)
            }

        } catch (e: ClassNotFoundException) {
            logger.error(e) {
                "이벤트 클래스를 찾을 수 없음: eventType=$eventType"
            }
            // DLQ로 이동하거나 재시도 로직 필요
        } catch (e: Exception) {
            logger.error(e) {
                "CDC 이벤트 처리 실패: topic=$topic, sagaId=$sagaId"
            }
            // 예외 발생 시 Kafka가 자동으로 재시도
            throw e
        }
    }

    /**
     * 토픽 이름에서 이벤트 타입 추출
     *
     * 예: shoot.events.MessageSentEvent → com.stark.shoot.domain.event.MessageSentEvent
     */
    private fun extractEventTypeFromTopic(topic: String): String {
        val eventName = topic.substringAfterLast(".")
        return "com.stark.shoot.domain.event.$eventName"
    }

    /**
     * Outbox 테이블에서 해당 이벤트를 처리 완료로 표시
     *
     * CDC가 처리한 이벤트는 OutboxEventProcessor가 다시 처리하지 않도록 함
     * 동일 Saga에 여러 이벤트가 있을 수 있으므로 event_type도 확인
     */
    private fun markAsProcessedBySagaId(sagaId: String, eventType: String) {
        try {
            val events = outboxEventRepository.findBySagaIdOrderByCreatedAtAsc(sagaId)

            events
                .filter { it.eventType == eventType && !it.processed }
                .forEach { event ->
                    event.markAsProcessed()
                    outboxEventRepository.save(event)
                    logger.debug {
                        "Outbox 이벤트 처리 완료 표시: id=${event.id}, sagaId=$sagaId"
                    }
                }
        } catch (e: Exception) {
            // Outbox 업데이트 실패는 로그만 남기고 무시
            // 중요한 것은 이벤트 발행이므로, 업데이트 실패해도 계속 진행
            logger.warn(e) {
                "Outbox 업데이트 실패 (무시됨): sagaId=$sagaId, eventType=$eventType"
            }
        }
    }
}
