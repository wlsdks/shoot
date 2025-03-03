package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface FriendUseCase {
    fun sendFriendRequest(currentUserId: ObjectId, targetUserId: ObjectId)
    fun acceptFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
    fun rejectFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
    fun removeFriend(userId: ObjectId, friendId: ObjectId): User
    fun searchPotentialFriends(currentUserId: ObjectId, query: String): List<FriendResponse>
}