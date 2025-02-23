package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import org.bson.types.ObjectId

interface ManageFriendUseCase {
    fun getFriends(currentUserId: ObjectId): List<FriendResponse> // 친구 목록 조회
    fun getIncomingFriendRequests(currentUserId: ObjectId): List<FriendResponse>
    fun getOutgoingFriendRequests(currentUserId: ObjectId): List<FriendResponse>

    fun sendFriendRequest(currentUserId: ObjectId, targetUserId: ObjectId)
    fun acceptFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
    fun rejectFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
    fun searchPotentialFriends(currentUserId: ObjectId, query: String): List<FriendResponse>
}