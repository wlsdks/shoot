package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId

/**
 * Command for deleting a message
 */
data class DeleteMessageCommand(
    val messageId: MessageId
) {
    companion object {
        fun of(messageId: String): DeleteMessageCommand {
            return DeleteMessageCommand(
                messageId = MessageId.from(messageId)
            )
        }
    }
}