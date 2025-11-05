package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for getting chat rooms for a user
 */
data class GetChatRoomsCommand(
    val userId: UserId
) {
    companion object {
        fun of(userId: Long): GetChatRoomsCommand {
            return GetChatRoomsCommand(
                userId = UserId.from(userId)
            )
        }
    }
}