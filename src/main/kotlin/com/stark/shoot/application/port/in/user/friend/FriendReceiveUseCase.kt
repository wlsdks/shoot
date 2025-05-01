package com.stark.shoot.application.port.`in`.user.friend

interface FriendReceiveUseCase {
    fun acceptFriendRequest(currentUserId: Long, requesterId: Long)
    fun rejectFriendRequest(currentUserId: Long, requesterId: Long)
}
