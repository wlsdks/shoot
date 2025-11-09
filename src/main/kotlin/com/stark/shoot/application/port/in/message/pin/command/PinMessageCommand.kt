package com.stark.shoot.application.port.`in`.message.pin.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId
import org.springframework.security.core.Authentication

/**
 * Command for pinning a message
 */
data class PinMessageCommand(
    val messageId: MessageId,
    val userId: UserId
) {
    companion object {
        fun of(messageId: String, userId: Long): PinMessageCommand {
            return PinMessageCommand(
                messageId = MessageId.from(messageId),
                userId = UserId.from(userId)
            )
        }
        
        fun of(messageId: String, authentication: Authentication): PinMessageCommand {
            val userId = authentication.name.toLong()
            return of(messageId, userId)
        }
    }
}