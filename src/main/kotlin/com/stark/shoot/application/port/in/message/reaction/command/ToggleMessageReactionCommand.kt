package com.stark.shoot.application.port.`in`.message.reaction.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

/**
 * Command for toggling a reaction on a message
 */
data class ToggleMessageReactionCommand(
    val messageId: MessageId,
    val userId: UserId,
    val reactionType: String
) {
    companion object {
        fun of(messageId: String, userId: Long, reactionType: String): ToggleMessageReactionCommand {
            return ToggleMessageReactionCommand(
                messageId = MessageId.from(messageId),
                userId = UserId.from(userId),
                reactionType = reactionType
            )
        }
        
        fun of(messageId: String, authentication: Authentication, reactionType: String): ToggleMessageReactionCommand {
            val userId = authentication.name.toLong()
            return of(messageId, userId, reactionType)
        }
    }
}