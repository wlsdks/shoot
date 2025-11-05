package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for searching friends
 */
data class SearchFriendsCommand(
    val userId: UserId,
    val query: String
) {
    companion object {
        fun of(userId: Long, query: String): SearchFriendsCommand {
            return SearchFriendsCommand(
                userId = UserId.from(userId),
                query = query
            )
        }
    }
}