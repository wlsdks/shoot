package com.stark.shoot.application.port.out.user.token

import com.stark.shoot.domain.user.RefreshToken
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.exception.web.InvalidRefreshTokenException
import com.stark.shoot.domain.exception.web.MongoOperationException
import java.time.Instant

interface RefreshTokenCommandPort {
    @Throws(MongoOperationException::class)
    fun createRefreshToken(
        userId: UserId,
        token: RefreshTokenValue,
        deviceInfo: String? = null,
        ipAddress: String? = null
    ): RefreshToken

    @Throws(InvalidRefreshTokenException::class, MongoOperationException::class)
    fun updateTokenUsage(token: RefreshTokenValue): RefreshToken?

    @Throws(InvalidRefreshTokenException::class, MongoOperationException::class)
    fun revokeToken(token: RefreshTokenValue): Boolean

    @Throws(MongoOperationException::class)
    fun revokeAllUserTokens(userId: UserId): Int

    @Throws(MongoOperationException::class)
    fun cleanupExpiredTokens(before: Instant = Instant.now()): Int
}
