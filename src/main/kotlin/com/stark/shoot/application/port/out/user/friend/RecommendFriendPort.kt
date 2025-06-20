package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.common.vo.UserId

interface RecommendFriendPort {
    fun recommendFriends(userId: UserId, limit: Int): List<User>
}