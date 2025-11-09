package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageRequest
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.springframework.security.core.Authentication
import java.time.Instant
import java.time.ZoneId

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

        fun of(request: ScheduleMessageRequest, authentication: Authentication): ScheduleMessageCommand {
            val userId = authentication.name.toLong()
            val scheduledInstant = request.scheduledAt.atZone(ZoneId.systemDefault()).toInstant()
            return of(request.roomId, userId, request.content, scheduledInstant)
        }
    }

}