package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for deleting a message
 */
data class DeleteMessageCommand(
    val messageId: MessageId,
    val userId: UserId // 웹소켓 응답을 위해 사용자 ID 추가
) {
    companion object {
        fun of(messageId: String, userId: Long): DeleteMessageCommand {
            return DeleteMessageCommand(
                messageId = MessageId.from(messageId),
                userId = UserId.from(userId)
            )
        }
    }
}