package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for creating a direct chat between two users
 */
data class CreateDirectChatCommand(
    val userId: UserId,
    val friendId: UserId
) {
    companion object {
        fun of(userId: Long, friendId: Long): CreateDirectChatCommand {
            return CreateDirectChatCommand(
                userId = UserId.from(userId),
                friendId = UserId.from(friendId)
            )
        }
    }
}