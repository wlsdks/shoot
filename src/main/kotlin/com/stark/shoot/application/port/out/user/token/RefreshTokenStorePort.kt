package com.stark.shoot.application.port.out.user.token

interface RefreshTokenStorePort {
    fun storeRefreshToken(userId: Long, refreshToken: String)
    fun isValidRefreshToken(refreshToken: String): Boolean
}