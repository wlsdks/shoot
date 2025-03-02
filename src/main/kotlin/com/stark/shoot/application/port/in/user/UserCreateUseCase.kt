package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.domain.chat.user.User

interface UserCreateUseCase {
    fun createUser(request: CreateUserRequest): User
}