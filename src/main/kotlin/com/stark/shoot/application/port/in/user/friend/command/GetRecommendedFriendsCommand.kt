package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for getting recommended friends for a user
 */
data class GetRecommendedFriendsCommand(
    val userId: UserId,
    val skip: Int,
    val limit: Int
) {
    companion object {
        fun of(userId: UserId, skip: Int, limit: Int): GetRecommendedFriendsCommand {
            return GetRecommendedFriendsCommand(userId, skip, limit)
        }
        
        fun of(userId: Long, skip: Int, limit: Int): GetRecommendedFriendsCommand {
            return GetRecommendedFriendsCommand(UserId.from(userId), skip, limit)
        }
    }
}