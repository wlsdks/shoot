package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.springframework.security.core.Authentication

/**
 * Command for getting scheduled messages
 */
data class GetScheduledMessagesCommand(
    val userId: UserId,
    val roomId: ChatRoomId?
) {
    companion object {
        fun of(userId: Long, roomId: Long?): GetScheduledMessagesCommand {
            return GetScheduledMessagesCommand(
                userId = UserId.from(userId),
                roomId = roomId?.let { ChatRoomId.from(it) }
            )
        }
        
        fun of(authentication: Authentication, roomId: Long?): GetScheduledMessagesCommand {
            val userId = authentication.name.toLong()
            return of(userId, roomId)
        }
    }
}