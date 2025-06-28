package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for finding a direct chat between two users
 */
data class FindDirectChatCommand(
    val userId1: UserId,
    val userId2: UserId
) {
    companion object {
        fun of(userId1: Long, userId2: Long): FindDirectChatCommand {
            return FindDirectChatCommand(
                userId1 = UserId.from(userId1),
                userId2 = UserId.from(userId2)
            )
        }
    }
}