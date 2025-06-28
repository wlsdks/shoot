package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

/**
 * Command for canceling a scheduled message
 */
data class CancelScheduledMessageCommand(
    val scheduledMessageId: String,
    val userId: UserId
) {
    companion object {
        fun of(scheduledMessageId: String, userId: Long): CancelScheduledMessageCommand {
            return CancelScheduledMessageCommand(
                scheduledMessageId = scheduledMessageId,
                userId = UserId.from(userId)
            )
        }
        
        fun of(scheduledMessageId: String, authentication: Authentication): CancelScheduledMessageCommand {
            val userId = authentication.name.toLong()
            return of(scheduledMessageId, userId)
        }
    }
}