package com.stark.shoot.application.port.`in`.user.token

import com.stark.shoot.application.dto.user.LoginResponse

interface RefreshTokenUseCase {
    fun generateNewAccessToken(refreshTokenHeader: String): LoginResponse
}