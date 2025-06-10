package com.stark.shoot.application.port.`in`.user.auth

import com.stark.shoot.application.dto.user.LoginRequest
import com.stark.shoot.application.dto.user.LoginResponse

interface UserLoginUseCase {
    fun login(request: LoginRequest): LoginResponse
}