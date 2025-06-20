package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.domain.common.vo.UserId

interface RecommendFriendsUseCase {
    fun getRecommendedFriends(userId: UserId, skip: Int, limit: Int): List<FriendResponse>
}