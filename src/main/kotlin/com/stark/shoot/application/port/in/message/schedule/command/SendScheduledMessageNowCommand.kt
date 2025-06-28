package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

/**
 * Command for sending a scheduled message immediately
 */
data class SendScheduledMessageNowCommand(
    val scheduledMessageId: String,
    val userId: UserId
) {
    companion object {
        fun of(scheduledMessageId: String, userId: Long): SendScheduledMessageNowCommand {
            return SendScheduledMessageNowCommand(
                scheduledMessageId = scheduledMessageId,
                userId = UserId.from(userId)
            )
        }
        
        fun of(scheduledMessageId: String, authentication: Authentication): SendScheduledMessageNowCommand {
            val userId = authentication.name.toLong()
            return of(scheduledMessageId, userId)
        }
    }
}