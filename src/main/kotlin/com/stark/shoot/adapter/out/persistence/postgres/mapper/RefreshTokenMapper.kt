package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import com.stark.shoot.domain.user.RefreshToken
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.stereotype.Component

@Component
class RefreshTokenMapper {

    /**
     * RefreshTokenEntity를 도메인 객체로 변환
     */
    fun toDomain(entity: RefreshTokenEntity): RefreshToken {
        return RefreshToken(
            id = entity.id,
            userId = UserId.from(entity.user.id),
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