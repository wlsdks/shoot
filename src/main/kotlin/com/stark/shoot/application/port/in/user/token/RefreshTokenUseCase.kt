package com.stark.shoot.application.port.`in`.user.token

interface RefreshTokenUseCase {
    fun isValidRefreshToken(refreshToken: String): Boolean
    fun getUserIdFromRefreshToken(refreshToken: String): String
}