package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import com.stark.shoot.domain.chat.user.RefreshToken
import com.stark.shoot.domain.chat.user.RefreshTokenValue
import org.springframework.stereotype.Component

@Component
class RefreshTokenMapper {

    /**
     * RefreshTokenEntity를 도메인 객체로 변환
     */
    fun toDomain(entity: RefreshTokenEntity): RefreshToken {
        return RefreshToken(
            id = entity.id,
            userId = entity.user.id,
            token = RefreshTokenValue.from(entity.token),
            expirationDate = entity.expirationDate,
            deviceInfo = entity.deviceInfo,
            ipAddress = entity.ipAddress,
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsedAt,
            isRevoked = entity.isRevoked
        )
    }

}