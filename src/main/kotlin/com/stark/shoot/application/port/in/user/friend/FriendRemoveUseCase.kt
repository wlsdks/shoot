package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.common.vo.UserId

interface FriendRemoveUseCase {
    fun removeFriend(userId: UserId, friendId: UserId): User
}