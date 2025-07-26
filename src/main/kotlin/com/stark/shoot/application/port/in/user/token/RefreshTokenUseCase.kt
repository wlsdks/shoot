package com.stark.shoot.application.port.`in`.user.token

import com.stark.shoot.adapter.`in`.rest.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.command.RefreshTokenCommand

interface RefreshTokenUseCase {
    fun generateNewAccessToken(command: RefreshTokenCommand): LoginResponse
}
