package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.common.vo.UserId

interface UpdateFriendPort {
    fun addOutgoingFriendRequest(userId: UserId, targetUserId: UserId)
    fun removeOutgoingFriendRequest(userId: UserId, targetUserId: UserId)
    fun removeIncomingFriendRequest(userId: UserId, fromUserId: UserId)
    fun addFriendRelation(userId: UserId, friendId: UserId)
    fun removeFriendRelation(userId: UserId, friendId: UserId)
}