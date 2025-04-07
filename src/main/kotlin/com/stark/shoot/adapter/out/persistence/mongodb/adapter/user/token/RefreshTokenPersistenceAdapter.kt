package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.token

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.RefreshTokenMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.RefreshTokenRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.token.RefreshTokenPort
import com.stark.shoot.domain.chat.user.RefreshToken
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Adapter
@Transactional
class RefreshTokenPersistenceAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val refreshTokenMapper: RefreshTokenMapper
) : RefreshTokenPort {

    /**
     * 새 리프레시 토큰 생성 및 저장
     */
    override fun createRefreshToken(
        userId: Long,
        token: String,
        deviceInfo: String?,
        ipAddress: String?
    ): RefreshToken {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        // 토큰 만료 시간 설정 (30일)
        val expirationDate = Instant.now().plusSeconds(30 * 24 * 60 * 60)

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

    /**
     * 토큰 문자열로 리프레시 토큰 조회
     */
    override fun findByToken(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)?.let {
            refreshTokenMapper.toDomain(it)
        }
    }

    /**
     * 리프레시 토큰 사용 시간 업데이트
     */
    override fun updateTokenUsage(token: String): RefreshToken? {
        val tokenEntity = refreshTokenRepository.findByToken(token) ?: return null

        // 사용 시간 업데이트
        tokenEntity.lastUsedAt = Instant.now()

        val updatedEntity = refreshTokenRepository.save(tokenEntity)
        return refreshTokenMapper.toDomain(updatedEntity)
    }

    /**
     * 리프레시 토큰 취소
     */
    override fun revokeToken(token: String): Boolean {
        val tokenEntity = refreshTokenRepository.findByToken(token) ?: return false

        // 토큰 취소 처리
        tokenEntity.isRevoked = true
        refreshTokenRepository.save(tokenEntity)

        return true
    }

    /**
     * 사용자의 모든 리프레시 토큰 취소
     */
    override fun revokeAllUserTokens(userId: Long): Int {
        return refreshTokenRepository.revokeAllByUserId(userId)
    }

    /**
     * 만료된 리프레시 토큰 정리
     */
    override fun cleanupExpiredTokens(before: Instant): Int {
        return refreshTokenRepository.deleteAllByExpirationDateBeforeOrIsRevokedTrue(before)
    }

}