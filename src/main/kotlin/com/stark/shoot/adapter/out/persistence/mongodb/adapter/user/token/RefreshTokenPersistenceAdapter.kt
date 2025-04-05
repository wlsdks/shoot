package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user.token

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.RefreshTokenMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.RefreshTokenRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.token.RefreshTokenPort
import com.stark.shoot.domain.chat.user.RefreshToken
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import java.time.Instant

@Adapter
class RefreshTokenPersistenceAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val refreshTokenMapper: RefreshTokenMapper
) : RefreshTokenPort {

    override fun createRefreshToken(
        userId: Long,
        token: String,
        deviceInfo: String?,
        ipAddress: String?
    ): RefreshToken {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val expirationDate = Instant.now().plusSeconds(30 * 24 * 60 * 60) // 30일

        val refreshTokenEntity = RefreshTokenEntity(
            user = userEntity,
            token = token,
            expirationDate = expirationDate,
            deviceInfo = deviceInfo,
            ipAddress = ipAddress,
            lastUsedAt = null,
            isRevoked = false
        )

        val savedEntity = refreshTokenRepository.save(refreshTokenEntity)
        return refreshTokenMapper.toDomain(savedEntity)
    }

    override fun findByToken(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)?.let {
            refreshTokenMapper.toDomain(it)
        }
    }

    override fun findAllByUserId(userId: Long): List<RefreshToken> {
        return refreshTokenRepository.findAllByUserId(userId).map {
            refreshTokenMapper.toDomain(it)
        }
    }

    override fun saveRefreshToken(refreshToken: RefreshToken): RefreshToken {
        // 기존 엔티티 찾기
        val entity = refreshToken.id?.let { id ->
            refreshTokenRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("RefreshToken not found: $id") }
        } ?: throw IllegalArgumentException("RefreshToken ID cannot be null for update operation")

        // 엔티티 속성 업데이트
        val updatedEntity = RefreshTokenEntity(
            user = entity.user,
            token = refreshToken.token,
            expirationDate = refreshToken.expirationDate,
            deviceInfo = refreshToken.deviceInfo,
            ipAddress = refreshToken.ipAddress,
            lastUsedAt = refreshToken.lastUsedAt,
            isRevoked = refreshToken.isRevoked
        )

        val savedEntity = refreshTokenRepository.save(updatedEntity)
        return refreshTokenMapper.toDomain(savedEntity)
    }

    override fun deleteByToken(token: String) {
        refreshTokenRepository.deleteByToken(token)
    }

    override fun deleteAllByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

}