package com.stark.shoot.application.port.out.user

import org.bson.types.ObjectId

interface UpdateUserFriendPort {
    fun addOutgoingFriendRequest(userId: ObjectId, targetUserId: ObjectId)
    fun addIncomingFriendRequest(userId: ObjectId, fromUserId: ObjectId)
    fun removeOutgoingFriendRequest(userId: ObjectId, targetUserId: ObjectId)
    fun removeIncomingFriendRequest(userId: ObjectId, fromUserId: ObjectId)

    fun addFriendRelation(userId: ObjectId, friendId: ObjectId)

    // 필요 시 “removeFriendRelation” 등 추가
}