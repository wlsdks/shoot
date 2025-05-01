package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.domain.chat.user.User

interface FriendRemoveUseCase {
    fun removeFriend(userId: Long, friendId: Long): User
}