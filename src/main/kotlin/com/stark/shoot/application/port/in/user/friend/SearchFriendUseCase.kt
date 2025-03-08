package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface SearchFriendUseCase {
    fun searchPotentialFriends(currentUserId: ObjectId, query: String): List<FriendResponse>
}