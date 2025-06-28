package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for getting friends of a user
 */
data class GetFriendsCommand(
    val currentUserId: UserId
) {
    companion object {
        fun of(currentUserId: UserId): GetFriendsCommand {
            return GetFriendsCommand(currentUserId)
        }
        
        fun of(currentUserId: Long): GetFriendsCommand {
            return GetFriendsCommand(UserId.from(currentUserId))
        }
    }
}