package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.chat.user.User

interface RecommendFriendPort {
    fun recommendFriends(userId: Long, limit: Int): List<User>
}