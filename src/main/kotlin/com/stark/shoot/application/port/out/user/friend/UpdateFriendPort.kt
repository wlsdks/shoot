package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface UpdateFriendPort {
    fun addOutgoingFriendRequest(userId: ObjectId, targetUserId: ObjectId)
    fun addIncomingFriendRequest(userId: ObjectId, fromUserId: ObjectId)
    fun removeOutgoingFriendRequest(userId: ObjectId, targetUserId: ObjectId)
    fun removeIncomingFriendRequest(userId: ObjectId, fromUserId: ObjectId)
    fun addFriendRelation(userId: ObjectId, friendId: ObjectId)
    fun updateFriends(user: User): User
}