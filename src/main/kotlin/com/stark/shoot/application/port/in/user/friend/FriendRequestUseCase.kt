package com.stark.shoot.application.port.`in`.user.friend

interface FriendRequestUseCase {
    fun sendFriendRequest(currentUserId: Long, targetUserId: Long)
    fun cancelFriendRequest(currentUserId: Long, targetUserId: Long)
}
