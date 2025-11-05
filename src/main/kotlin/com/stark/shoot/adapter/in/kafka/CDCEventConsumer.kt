package com.stark.shoot.adapter.`in`.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.domain.shared.event.DomainEvent
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
 * - Topic: shoot.cdc.public.outbox_events (단일 토픽 방식)
 * - Debezium의 Simple CDC 구현 (EventRouter 없음)
 *
 * **동작 방식**:
 * 1. Debezium이 PostgreSQL WAL에서 Outbox 테이블 변경 감지
 * 2. 변경사항을 Debezium 표준 형식으로 Kafka 발행 (before/after/source 구조)
 * 3. 이 Consumer가 메시지에서 after 필드 추출
 * 4. event_type 기반으로 실제 비즈니스 이벤트를 내부 Kafka 토픽으로 재발행
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
     * CDC 이벤트 소비 (Simple CDC - EventRouter 없음)
     *
     * Debezium 표준 형식:
     * - Topic: shoot.cdc.public.outbox_events
     * - Payload: { "before": null, "after": {...}, "source": {...}, "op": "c" }
     * - after 필드에 outbox_events 테이블 레코드 포함
     *
     * @param debeziumMessage Debezium 전체 메시지 (JSON)
     * @param topic Kafka 토픽
     * @param partition 파티션 번호
     * @param offset 오프셋
     */
    @KafkaListener(
        topics = ["shoot.cdc.public.outbox_events"],
        groupId = "shoot-cdc-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun consumeCDCEvent(
        @Payload debeziumMessage: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        logger.info {
            "CDC 이벤트 수신: topic=$topic, partition=$partition, offset=$offset"
        }

        try {
            // 1. Debezium 메시지 파싱
            val debeziumPayload = objectMapper.readTree(debeziumMessage)
            val operation = debeziumPayload.get("op")?.asText()

            // INSERT, UPDATE만 처리 (DELETE는 무시)
            if (operation != "c" && operation != "u") {
                logger.debug { "CDC 이벤트 스킵 (op=$operation)" }
                return
            }

            val afterNode = debeziumPayload.get("after") ?: run {
                logger.warn { "CDC 메시지에 'after' 필드 없음" }
                return
            }

            // 2. Outbox 이벤트 정보 추출
            val sagaId = afterNode.get("saga_id")?.asText()
            val eventType = afterNode.get("event_type")?.asText()
            val payloadJson = afterNode.get("payload")?.asText()
            val processed = afterNode.get("processed")?.asBoolean() ?: false

            // 이미 처리된 이벤트는 스킵
            if (processed) {
                logger.debug { "이미 처리된 CDC 이벤트 스킵: sagaId=$sagaId" }
                return
            }

            if (eventType == null || payloadJson == null) {
                logger.warn { "CDC 메시지에 필수 필드 없음: eventType=$eventType, payload=$payloadJson" }
                return
            }

            // 3. 이벤트 역직렬화
            val eventClass = Class.forName(eventType)
            val event = objectMapper.readValue(payloadJson, eventClass) as DomainEvent

            // 4. 실제 비즈니스 이벤트 발행
            eventPublisher.publishEvent(event)

            logger.info {
                "CDC 이벤트 처리 완료: eventType=$eventType, sagaId=$sagaId"
            }

            // 5. Outbox 테이블 업데이트 (processed=true)
            if (sagaId != null) {
                markAsProcessedBySagaId(sagaId, eventType)
            }

        } catch (e: ClassNotFoundException) {
            logger.error(e) {
                "이벤트 클래스를 찾을 수 없음: message=${debeziumMessage.take(200)}"
            }
            // DLQ로 이동하거나 재시도 로직 필요
        } catch (e: Exception) {
            logger.error(e) {
                "CDC 이벤트 처리 실패: topic=$topic, message=${debeziumMessage.take(200)}"
            }
            // 예외 발생 시 Kafka가 자동으로 재시도
            throw e
        }
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
