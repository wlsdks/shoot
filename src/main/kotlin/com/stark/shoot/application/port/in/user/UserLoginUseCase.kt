package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse

interface UserLoginUseCase {
    fun login(request: LoginRequest): LoginResponse
}