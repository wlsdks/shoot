package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserId

interface RecommendFriendPort {
    fun recommendFriends(userId: UserId, limit: Int): List<User>
}