package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for accepting a friend request
 */
data class AcceptFriendRequestCommand(
    val currentUserId: UserId,
    val requesterId: UserId
) {
    companion object {
        fun of(currentUserId: Long, requesterId: Long): AcceptFriendRequestCommand {
            return AcceptFriendRequestCommand(
                currentUserId = UserId.from(currentUserId),
                requesterId = UserId.from(requesterId)
            )
        }
    }
}