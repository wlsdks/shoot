package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import org.bson.types.ObjectId

interface FindFriendUseCase {
    fun getFriends(currentUserId: ObjectId): List<FriendResponse> // 친구 목록 조회
    fun getIncomingFriendRequests(currentUserId: ObjectId): List<FriendResponse>
    fun getOutgoingFriendRequests(currentUserId: ObjectId): List<FriendResponse>
}