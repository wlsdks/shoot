package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserId

interface FriendRemoveUseCase {
    fun removeFriend(userId: UserId, friendId: UserId): User
}