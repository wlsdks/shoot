package com.stark.shoot.application.service.saga.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxEventEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.domain.shared.event.MentionEvent
import com.stark.shoot.domain.shared.event.MessageSentEvent
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 3: Outbox에 이벤트 저장
 *
 * PostgreSQL 트랜잭션 내에서 이벤트를 저장하여 이벤트 발행을 보장합니다.
 *
 * @Transactional(MANDATORY): Step 2에서 시작된 트랜잭션에 필수로 참여
 * Step 2와 함께 커밋/롤백되어 데이터 일관성을 보장합니다.
 */
@Component
class PublishEventToOutboxStep(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val userQueryPort: com.stark.shoot.application.port.out.user.UserQueryPort
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    @Transactional(propagation = Propagation.MANDATORY)  // 기존 트랜잭션 필수
    override fun execute(context: MessageSagaContext): Boolean {
        return try {
            val savedMessage = context.savedMessage
                ?: throw IllegalStateException("Message not saved yet")

            // 1. MessageSentEvent 발행
            val messageSentEvent = MessageSentEvent.create(savedMessage)
            saveToOutbox(context.sagaId, messageSentEvent)

            // 2. 멘션이 있으면 MentionEvent 발행
            if (savedMessage.mentions.isNotEmpty()) {
                val mentionEvent = createMentionEvent(savedMessage)
                if (mentionEvent != null) {
                    saveToOutbox(context.sagaId, mentionEvent)
                }
            }

            context.recordStep(stepName())
            logger.info { "Events saved to outbox for saga: ${context.sagaId}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save events to outbox" }
            context.markFailed(e)
            false
        }
    }

    override fun compensate(context: MessageSagaContext): Boolean {
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

    override fun stepName() = "PublishEventToOutbox"

    /**
     * Outbox에 이벤트 저장
     * Idempotency 보장: 중복 이벤트 생성 방지
     */
    private fun saveToOutbox(sagaId: String, event: Any) {
        val eventTypeName = event::class.java.simpleName
        val idempotencyKey = "$sagaId-$eventTypeName"

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

    /**
     * 멘션 이벤트 생성
     */
    private fun createMentionEvent(message: com.stark.shoot.domain.chat.message.ChatMessage): MentionEvent? {
        // 자신을 멘션한 경우는 제외
        val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()
        if (mentionedUsers.isEmpty()) {
            return null
        }

        // 발신자 정보 조회
        val senderName = userQueryPort
            .findUserById(message.senderId)
            ?.nickname
            ?.value
            ?: "User_${message.senderId.value}"

        return MentionEvent(
            roomId = message.roomId,
            messageId = message.id ?: return null,
            senderId = message.senderId,
            senderName = senderName,
            mentionedUserIds = mentionedUsers,
            messageContent = message.content.text
        )
    }
}
