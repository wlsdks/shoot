package com.stark.shoot.application.port.`in`.user.friend

interface FriendRequestUseCase {
    fun sendFriendRequest(currentUserId: Long, targetUserId: Long)
    fun acceptFriendRequest(currentUserId: Long, requesterId: Long)
    fun rejectFriendRequest(currentUserId: Long, requesterId: Long)
}