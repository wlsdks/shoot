package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for removing a participant from a chat room
 */
data class RemoveParticipantCommand(
    val roomId: ChatRoomId,
    val userId: UserId
) {
    companion object {
        fun of(roomId: Long, userId: Long): RemoveParticipantCommand {
            return RemoveParticipantCommand(
                roomId = ChatRoomId.from(roomId),
                userId = UserId.from(userId)
            )
        }
    }
}