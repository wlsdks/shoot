package com.stark.shoot.domain.user

import java.time.Instant
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.user.vo.UserId

data class RefreshToken(
    val id: Long? = null,
    val userId: UserId,                 // 사용자 ID 참조
    val token: RefreshTokenValue,     // 리프레시 토큰 값
    val expirationDate: Instant,      // 만료 시간
    val deviceInfo: String? = null,   // 디바이스 정보 (선택적)
    val ipAddress: String? = null,    // IP 주소 (선택적)
    val createdAt: Instant = Instant.now(),  // 발급 시간
    val lastUsedAt: Instant? = null,  // 마지막 사용 시간
    val isRevoked: Boolean = false    // 취소 여부
) {
    // 토큰이 유효한지 확인
    fun isValid(): Boolean {
        return !isRevoked && expirationDate.isAfter(Instant.now())
    }

    // 토큰 사용 시간 업데이트
    fun updateLastUsed() = copy(lastUsedAt = Instant.now())

    // 토큰 취소
    fun revoke() = copy(isRevoked = true)
}