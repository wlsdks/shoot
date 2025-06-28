package com.stark.shoot.application.port.`in`.message.mark.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for marking all messages in a chat room as read
 */
data class MarkAllMessagesAsReadCommand(
    val roomId: ChatRoomId,
    val userId: UserId,
    val requestId: String?
) {
    companion object {
        /**
         * Factory method to create a MarkAllMessagesAsReadCommand
         *
         * @param roomId The ID of the chat room
         * @param userId The ID of the user who read the messages
         * @param requestId Optional request ID for tracking
         * @return A new MarkAllMessagesAsReadCommand
         */
        fun of(roomId: Long, userId: Long, requestId: String?): MarkAllMessagesAsReadCommand {
            return MarkAllMessagesAsReadCommand(
                roomId = ChatRoomId.from(roomId),
                userId = UserId.from(userId),
                requestId = requestId
            )
        }
    }
}