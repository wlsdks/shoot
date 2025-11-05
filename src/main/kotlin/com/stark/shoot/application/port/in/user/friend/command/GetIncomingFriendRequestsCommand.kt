package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for getting incoming friend requests of a user
 */
data class GetIncomingFriendRequestsCommand(
    val currentUserId: UserId
) {
    companion object {
        fun of(currentUserId: UserId): GetIncomingFriendRequestsCommand {
            return GetIncomingFriendRequestsCommand(currentUserId)
        }
        
        fun of(currentUserId: Long): GetIncomingFriendRequestsCommand {
            return GetIncomingFriendRequestsCommand(UserId.from(currentUserId))
        }
    }
}