package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.command.GetRecommendedFriendsCommand

interface RecommendFriendsUseCase {
    fun getRecommendedFriends(command: GetRecommendedFriendsCommand): List<FriendResponse>
}
