package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse

interface SearchFriendUseCase {
    fun searchPotentialFriends(currentUserId: Long, query: String): List<FriendResponse>
}