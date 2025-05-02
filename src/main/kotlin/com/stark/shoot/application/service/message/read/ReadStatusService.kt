package com.stark.shoot.application.service.message.read

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.read.ReadStatusUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.ReadStatusPort
import com.stark.shoot.application.service.message.mark.MarkMessageReadService
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadStatusService(
    private val readStatusPort: ReadStatusPort,
    private val eventPublisher: EventPublisher,
    private val loadMessagePort: LoadMessagePort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val markMessageReadService: MarkMessageReadService
) : ReadStatusUseCase {

    @Transactional
    override fun markMessageAsRead(messageId: String?, userId: Long) {
        if (messageId == null) {
            throw IllegalArgumentException("Message ID cannot be null.")
        }

        // Use MarkMessageReadService to handle read status
        markMessageReadService.markMessageAsRead(messageId, userId)
    }

    @Transactional
    override fun markAllMessagesAsRead(roomId: Long, userId: Long, requestId: String?) {
        // Use MarkMessageReadService to handle read status
        markMessageReadService.markAllMessagesAsRead(roomId, userId, requestId)
    }

    @Transactional
    override fun incrementUnreadCount(roomId: Long, userId: Long) {
        // Increment unread count
        val updatedStatus = readStatusPort.incrementUnreadCount(roomId, userId)

        // Publish event
        publishUnreadCountEvent(roomId)
    }

    private fun publishUnreadCountEvent(roomId: Long) {
        // Get read status for all participants in the chat room
        val readStatuses = readStatusPort.findAllByRoomId(roomId)

        // Create map of unread counts
        val unreadCounts = readStatuses.associate { it.userId to it.unreadCount }

        // Publish event
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomId,
                unreadCounts = unreadCounts,
                lastMessage = null // Set last message as needed
            )
        )
    }

    private fun getRoomIdFromMessageId(messageId: String?): Long {
        if (messageId == null) {
            throw IllegalArgumentException("Message ID cannot be null.")
        }

        try {
            val objectId = ObjectId(messageId)
            val message = loadMessagePort.findById(objectId)
                ?: throw IllegalArgumentException("Message with ID $messageId not found.")

            return message.roomId
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid message ID format: $messageId", e)
        } catch (e: Exception) {
            throw RuntimeException("Error retrieving room ID from message ID.", e)
        }
    }

}