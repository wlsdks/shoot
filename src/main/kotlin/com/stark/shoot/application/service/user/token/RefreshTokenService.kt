package com.stark.shoot.application.service.user.token

import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import com.stark.shoot.application.port.out.user.token.RefreshTokenStorePort
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    private val refreshTokenStorePort: RefreshTokenStorePort
) : RefreshTokenUseCase {

    override fun isValidRefreshToken(
        refreshToken: String
    ): Boolean {
        return refreshTokenStorePort.isValidRefreshToken(refreshToken)
    }

    override fun getUserIdFromRefreshToken(
        refreshToken: String
    ): String {
        return refreshTokenStorePort.getUserIdFromRefreshToken(refreshToken)
    }

}