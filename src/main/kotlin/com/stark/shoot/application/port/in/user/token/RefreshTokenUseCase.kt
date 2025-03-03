package com.stark.shoot.application.port.`in`.user.token

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse

interface RefreshTokenUseCase {
    fun generateNewAccessToken(refreshTokenHeader: String): LoginResponse
}