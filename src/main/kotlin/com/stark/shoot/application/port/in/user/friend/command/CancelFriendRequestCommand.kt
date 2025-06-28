package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for canceling a friend request
 */
data class CancelFriendRequestCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {
    companion object {
        fun of(currentUserId: Long, targetUserId: Long): CancelFriendRequestCommand {
            return CancelFriendRequestCommand(
                currentUserId = UserId.from(currentUserId),
                targetUserId = UserId.from(targetUserId)
            )
        }
    }
}