package com.stark.shoot.application.port.`in`.user

import org.bson.types.ObjectId

interface ManageFriendUseCase {
    fun getFriends(currentUserId: ObjectId): List<ObjectId> // 친구 목록 조회
    fun getIncomingFriendRequests(currentUserId: ObjectId): List<ObjectId>
    fun getOutgoingFriendRequests(currentUserId: ObjectId): List<ObjectId>

    fun sendFriendRequest(currentUserId: ObjectId, targetUserId: ObjectId)
    fun acceptFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
    fun rejectFriendRequest(currentUserId: ObjectId, requesterId: ObjectId)
}