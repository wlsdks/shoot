package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Command for scheduling a message
 */
data class ScheduleMessageCommand(
    val roomId: ChatRoomId,
    val senderId: UserId,
    val content: String,
    val scheduledAt: Instant
) {
    companion object {
        fun of(roomId: Long, senderId: Long, content: String, scheduledAt: Instant): ScheduleMessageCommand {
            return ScheduleMessageCommand(
                roomId = ChatRoomId.from(roomId),
                senderId = UserId.from(senderId),
                content = content,
                scheduledAt = scheduledAt
            )
        }
        
        fun of(roomId: Long, authentication: Authentication, content: String, scheduledAt: LocalDateTime): ScheduleMessageCommand {
            val userId = authentication.name.toLong()
            val scheduledInstant = scheduledAt.atZone(ZoneId.systemDefault()).toInstant()
            return of(roomId, userId, content, scheduledInstant)
        }
    }
}