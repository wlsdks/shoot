package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for sending a friend request
 */
data class SendFriendRequestCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {
    companion object {
        fun of(currentUserId: Long, targetUserId: Long): SendFriendRequestCommand {
            return SendFriendRequestCommand(
                currentUserId = UserId.from(currentUserId),
                targetUserId = UserId.from(targetUserId)
            )
        }
    }
}