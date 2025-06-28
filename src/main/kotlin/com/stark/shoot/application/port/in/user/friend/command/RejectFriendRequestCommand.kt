package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for rejecting a friend request
 */
data class RejectFriendRequestCommand(
    val currentUserId: UserId,
    val requesterId: UserId
) {
    companion object {
        fun of(currentUserId: Long, requesterId: Long): RejectFriendRequestCommand {
            return RejectFriendRequestCommand(
                currentUserId = UserId.from(currentUserId),
                requesterId = UserId.from(requesterId)
            )
        }
    }
}