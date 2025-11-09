package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

@AggregateRoot
data class RefreshToken(
    val id: Long? = null,
    val userId: UserId,                 // 사용자 ID 참조
    val token: RefreshTokenValue,     // 리프레시 토큰 값
    var expirationDate: Instant,      // 만료 시간
    var deviceInfo: String? = null,   // 디바이스 정보 (선택적)
    var ipAddress: String? = null,    // IP 주소 (선택적)
    var createdAt: Instant = Instant.now(),  // 발급 시간
    var lastUsedAt: Instant? = null,  // 마지막 사용 시간
    var isRevoked: Boolean = false    // 취소 여부
) {
    // 토큰이 유효한지 확인
    fun isValid(): Boolean {
        return !isRevoked && expirationDate.isAfter(Instant.now())
    }

    // 토큰 사용 시간 업데이트
    fun updateLastUsed() {
        lastUsedAt = Instant.now()
    }

    // 토큰 취소
    fun revoke() {
        isRevoked = true
    }
}