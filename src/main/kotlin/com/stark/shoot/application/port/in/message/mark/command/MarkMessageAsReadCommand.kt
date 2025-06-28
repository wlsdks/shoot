package com.stark.shoot.application.port.`in`.message.mark.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for marking a message as read
 */
data class MarkMessageAsReadCommand(
    val messageId: MessageId,
    val userId: UserId
) {
    companion object {
        /**
         * Factory method to create a MarkMessageAsReadCommand
         *
         * @param messageId The ID of the message to mark as read
         * @param userId The ID of the user who read the message
         * @return A new MarkMessageAsReadCommand
         */
        fun of(messageId: String, userId: Long): MarkMessageAsReadCommand {
            return MarkMessageAsReadCommand(
                messageId = MessageId.from(messageId),
                userId = UserId.from(userId)
            )
        }
    }
}