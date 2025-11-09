package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId

/**
 * Command for editing a message
 */
data class EditMessageCommand(
    val messageId: MessageId,
    val newContent: String,
    val userId: UserId // 웹소켓 응답을 위해 사용자 ID 추가
) {
    companion object {
        fun of(messageId: String, newContent: String, userId: Long): EditMessageCommand {
            return EditMessageCommand(
                messageId = MessageId.from(messageId),
                newContent = newContent,
                userId = UserId.from(userId)
            )
        }
    }
}