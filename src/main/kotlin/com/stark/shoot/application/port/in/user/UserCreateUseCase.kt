package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User

interface UserCreateUseCase {
    fun createUser(username: String, nickname: String): User
}