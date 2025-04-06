package com.stark.shoot.application.port.out.user.token

import com.stark.shoot.domain.chat.user.RefreshToken

interface RefreshTokenPort {
    fun createRefreshToken(
        userId: Long,
        token: String,
        deviceInfo: String? = null,
        ipAddress: String? = null
    ): RefreshToken

    fun findByToken(token: String): RefreshToken?
}