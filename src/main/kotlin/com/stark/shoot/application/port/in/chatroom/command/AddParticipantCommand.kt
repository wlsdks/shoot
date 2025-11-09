package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * Command for adding a participant to a chat room
 */
data class AddParticipantCommand(
    val roomId: ChatRoomId,
    val userId: UserId
) {
    companion object {
        fun of(roomId: Long, userId: Long): AddParticipantCommand {
            return AddParticipantCommand(
                roomId = ChatRoomId.from(roomId),
                userId = UserId.from(userId)
            )
        }
    }
}