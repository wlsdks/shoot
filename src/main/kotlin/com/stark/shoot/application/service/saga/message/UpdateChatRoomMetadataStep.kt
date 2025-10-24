package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Saga Step 2: PostgreSQL 채팅방 메타데이터 업데이트
 */
@Component
class UpdateChatRoomMetadataStep(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    override fun execute(context: MessageSagaContext): Boolean {
        return try {
            val savedMessage = context.savedMessage
                ?: throw IllegalStateException("Message not saved yet")

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
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to update chatroom metadata" }
            context.markFailed(e)
            false
        }
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
