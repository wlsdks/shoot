package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.domain.user.vo.UserId

interface FriendSearchUseCase {
    fun searchPotentialFriends(currentUserId: UserId, query: String): List<FriendResponse>
}