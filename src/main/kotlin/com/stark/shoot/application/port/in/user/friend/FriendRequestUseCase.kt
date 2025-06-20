package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.domain.common.vo.UserId

interface FriendRequestUseCase {
    fun sendFriendRequest(currentUserId: UserId, targetUserId: UserId)
    fun cancelFriendRequest(currentUserId: UserId, targetUserId: UserId)
}
