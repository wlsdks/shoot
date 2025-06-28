package com.stark.shoot.application.port.`in`.user.auth

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.auth.command.LoginCommand

interface UserLoginUseCase {
    fun login(command: LoginCommand): LoginResponse
}
