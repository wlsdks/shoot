package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Command for updating a scheduled message
 */
data class UpdateScheduledMessageCommand(
    val scheduledMessageId: String,
    val userId: UserId,
    val newContent: String,
    val newScheduledAt: Instant?
) {
    companion object {
        fun of(scheduledMessageId: String, userId: Long, newContent: String, newScheduledAt: Instant?): UpdateScheduledMessageCommand {
            return UpdateScheduledMessageCommand(
                scheduledMessageId = scheduledMessageId,
                userId = UserId.from(userId),
                newContent = newContent,
                newScheduledAt = newScheduledAt
            )
        }
        
        fun of(scheduledMessageId: String, authentication: Authentication, newContent: String, newScheduledAt: LocalDateTime?): UpdateScheduledMessageCommand {
            val userId = authentication.name.toLong()
            val newScheduledInstant = newScheduledAt?.atZone(ZoneId.systemDefault())?.toInstant()
            return of(scheduledMessageId, userId, newContent, newScheduledInstant)
        }
    }
}