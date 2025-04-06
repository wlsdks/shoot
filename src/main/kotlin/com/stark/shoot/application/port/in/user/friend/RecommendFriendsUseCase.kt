package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse

interface RecommendFriendsUseCase {
    fun getRecommendedFriends(userId: Long, skip: Int, limit: Int): List<FriendResponse>
}