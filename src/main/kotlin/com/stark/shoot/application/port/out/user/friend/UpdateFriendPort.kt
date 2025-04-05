package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.chat.user.User

interface UpdateFriendPort {
    fun addOutgoingFriendRequest(userId: Long, targetUserId: Long)
    fun removeOutgoingFriendRequest(userId: Long, targetUserId: Long)
    fun removeIncomingFriendRequest(userId: Long, fromUserId: Long)
    fun addFriendRelation(userId: Long, friendId: Long)
    fun updateFriends(user: User): User
    fun removeFriendRelation(userId: Long, friendId: Long)
}