package com.stark.shoot.application.service.saga.friend.steps

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxEventEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.service.saga.friend.FriendRequestSagaContext
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.shared.event.FriendAddedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 3: Outbox에 친구 추가 이벤트 저장
 *
 * PostgreSQL 트랜잭션 내에서 이벤트를 저장하여 이벤트 발행을 보장합니다.
 *
 * @Transactional(MANDATORY): Step 2에서 시작된 트랜잭션에 필수로 참여
 * Step 2와 함께 커밋/롤백되어 데이터 일관성을 보장합니다.
 */
@Component
class PublishFriendEventsStep(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) : SagaStep<FriendRequestSagaContext> {

    private val logger = KotlinLogging.logger {}

    @Transactional(propagation = Propagation.MANDATORY)  // 기존 트랜잭션 필수
    override fun execute(context: FriendRequestSagaContext): Boolean {
        return try {
            // 양방향 친구 추가 이벤트 발행
            // 1. receiverId → requesterId 친구 추가 이벤트
            val event1 = FriendAddedEvent.create(
                userId = context.receiverId,
                friendId = context.requesterId
            )
            saveToOutbox(context.sagaId, event1, suffix = "receiver")

            // 2. requesterId → receiverId 친구 추가 이벤트
            val event2 = FriendAddedEvent.create(
                userId = context.requesterId,
                friendId = context.receiverId
            )
            saveToOutbox(context.sagaId, event2, suffix = "requester")

            context.recordStep(stepName())
            logger.info { "Friend events saved to outbox for saga: ${context.sagaId}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save friend events to outbox" }
            context.markFailed(e)
            false
        }
    }

    /**
     * 보상: Outbox에 저장된 이벤트 삭제
     *
     * 이벤트는 아직 발행되지 않았으므로 삭제만 하면 됩니다.
     * (Outbox 패턴에서는 이벤트 발행 후 재처리는 불가능)
     */
    @Transactional(propagation = Propagation.MANDATORY)  // 기존 트랜잭션 필수
    override fun compensate(context: FriendRequestSagaContext): Boolean {
        return try {
            // Outbox 이벤트 삭제 (Saga ID로 조회해서 삭제)
            val events = outboxEventRepository.findBySagaIdOrderByCreatedAtAsc(context.sagaId)
            outboxEventRepository.deleteAll(events)
            logger.info { "Compensated: Deleted ${events.size} outbox events for saga: ${context.sagaId}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate outbox events" }
            false
        }
    }

    override fun stepName() = "PublishFriendEvents"

    /**
     * Outbox에 이벤트 저장
     * Idempotency 보장: 중복 이벤트 생성 방지
     *
     * @param sagaId Saga ID
     * @param event FriendAddedEvent
     * @param suffix Idempotency key에 추가할 suffix (receiver/requester 구분)
     */
    private fun saveToOutbox(sagaId: String, event: FriendAddedEvent, suffix: String) {
        val eventTypeName = event::class.java.simpleName
        val idempotencyKey = "$sagaId-$eventTypeName-$suffix"

        // Idempotency check: 이미 존재하는 이벤트는 저장하지 않음
        if (outboxEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            logger.info { "Outbox event already exists (skipping duplicate): $idempotencyKey" }
            return
        }

        try {
            val payload = objectMapper.writeValueAsString(event)
            val outboxEvent = OutboxEventEntity(
                sagaId = sagaId,
                idempotencyKey = idempotencyKey,
                eventType = event::class.java.name,
                payload = payload
            )
            outboxEventRepository.save(outboxEvent)
        } catch (e: org.springframework.dao.DataIntegrityViolationException) {
            // Race condition: check와 save 사이에 다른 스레드가 같은 이벤트 생성
            // DB 제약 조건이 중복을 방지했으므로 정상 처리
            logger.warn { "Duplicate outbox event caught by DB constraint: $idempotencyKey" }
            // 예외를 다시 던지지 않음 (이벤트는 이미 저장됨)
        }
    }
}
