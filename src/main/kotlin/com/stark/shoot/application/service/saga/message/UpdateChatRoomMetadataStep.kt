package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 2: PostgreSQL 채팅방 메타데이터 업데이트
 *
 * @Transactional: PostgreSQL 트랜잭션 시작
 * Step 3(Outbox)도 이 트랜잭션에 참여하여 함께 커밋/롤백됩니다.
 *
 * Optimistic Locking:
 * ChatRoomEntity에 @Version이 있어 동시 업데이트 시 OptimisticLockException 발생
 * 최대 3회 재시도하여 동시성 문제 해결
 */
@Component
class UpdateChatRoomMetadataStep(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 50L
    }

    @Transactional  // PostgreSQL 트랜잭션 시작
    override fun execute(context: MessageSagaContext): Boolean {
        val savedMessage = context.savedMessage
            ?: throw IllegalStateException("Message not saved yet")

        var retries = MAX_RETRIES
        while (retries > 0) {
            try {
                return executeInternal(context, savedMessage)
            } catch (e: OptimisticLockException) {
                retries--
                if (retries == 0) {
                    logger.error(e) { "OptimisticLockException after $MAX_RETRIES retries" }
                    context.markFailed(e)
                    return false
                }

                logger.warn { "OptimisticLockException occurred, retrying... ($retries left)" }
                Thread.sleep(RETRY_DELAY_MS)
            } catch (e: Exception) {
                logger.error(e) { "Failed to update chatroom metadata" }
                context.markFailed(e)
                return false
            }
        }

        return false
    }

    private fun executeInternal(
        context: MessageSagaContext,
        savedMessage: com.stark.shoot.domain.chat.message.ChatMessage
    ): Boolean {
        // 채팅방 조회
        val chatRoom = chatRoomQueryPort.findById(savedMessage.roomId)
        if (chatRoom == null) {
            logger.error { "ChatRoom not found: roomId=${savedMessage.roomId.value}" }
            return false
        }

        context.chatRoom = chatRoom

        // 메타데이터 업데이트
        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(chatRoom, savedMessage)
        val savedRoom = chatRoomCommandPort.save(updatedRoom)

        // 마지막 읽은 메시지 ID 업데이트
        savedMessage.id?.let { messageId ->
            chatRoomCommandPort.updateLastReadMessageId(
                savedMessage.roomId,
                savedMessage.senderId,
                messageId
            )
        }

        context.updatedChatRoom = savedRoom
        context.recordStep(stepName())

        logger.info { "ChatRoom metadata updated: roomId=${savedRoom.id?.value}" }
        return true
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        return try {
            val originalRoom = context.chatRoom
            if (originalRoom != null) {
                // 원래 상태로 복원
                chatRoomCommandPort.save(originalRoom)
                logger.info { "Compensated: Restored chatroom metadata: roomId=${originalRoom.id?.value}" }
            } else {
                logger.warn { "No original chatroom to restore" }
            }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate chatroom metadata update" }
            false
        }
    }

    override fun stepName() = "UpdateChatRoomMetadata"
}
