package com.stark.shoot.application.port.`in`.message.pin.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

/**
 * Command for unpinning a message
 */
data class UnpinMessageCommand(
    val messageId: MessageId,
    val userId: UserId
) {
    companion object {
        fun of(messageId: String, userId: Long): UnpinMessageCommand {
            return UnpinMessageCommand(
                messageId = MessageId.from(messageId),
                userId = UserId.from(userId)
            )
        }
        
        fun of(messageId: String, authentication: Authentication): UnpinMessageCommand {
            val userId = authentication.name.toLong()
            return of(messageId, userId)
        }
    }
}