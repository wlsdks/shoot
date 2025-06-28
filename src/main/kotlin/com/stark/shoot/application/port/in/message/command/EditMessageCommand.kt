package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId

/**
 * Command for editing a message
 */
data class EditMessageCommand(
    val messageId: MessageId,
    val newContent: String
) {
    companion object {
        fun of(messageId: String, newContent: String): EditMessageCommand {
            return EditMessageCommand(
                messageId = MessageId.from(messageId),
                newContent = newContent
            )
        }
    }
}