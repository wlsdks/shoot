package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.domain.user.vo.UserId

interface FriendReceiveUseCase {
    fun acceptFriendRequest(currentUserId: UserId, requesterId: UserId)
    fun rejectFriendRequest(currentUserId: UserId, requesterId: UserId)
}
