package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for getting outgoing friend requests of a user
 */
data class GetOutgoingFriendRequestsCommand(
    val currentUserId: UserId
) {
    companion object {
        fun of(currentUserId: UserId): GetOutgoingFriendRequestsCommand {
            return GetOutgoingFriendRequestsCommand(currentUserId)
        }
        
        fun of(currentUserId: Long): GetOutgoingFriendRequestsCommand {
            return GetOutgoingFriendRequestsCommand(UserId.from(currentUserId))
        }
    }
}