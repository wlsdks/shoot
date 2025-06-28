package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

/**
 * Command for removing a friend
 */
data class RemoveFriendCommand(
    val userId: UserId,
    val friendId: UserId
) {
    companion object {
        fun of(userId: Long, friendId: Long): RemoveFriendCommand {
            return RemoveFriendCommand(
                userId = UserId.from(userId),
                friendId = UserId.from(friendId)
            )
        }
        
        fun of(authentication: Authentication, friendId: Long): RemoveFriendCommand {
            val userId = authentication.name.toLong()
            return of(userId, friendId)
        }
    }
}