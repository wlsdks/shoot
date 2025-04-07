package com.stark.shoot.application.port.out.user.token

import com.stark.shoot.domain.chat.user.RefreshToken
import java.time.Instant

/**
 * 리프레시 토큰 관련 영속성 작업을 위한 출력 포트
 */
interface RefreshTokenPort {
    /**
     * 새 리프레시 토큰 생성
     *
     * @param userId 사용자 ID
     * @param token 토큰 문자열
     * @param deviceInfo 디바이스 정보 (선택)
     * @param ipAddress IP 주소 (선택)
     * @return 생성된 RefreshToken 객체
     */
    fun createRefreshToken(
        userId: Long,
        token: String,
        deviceInfo: String? = null,
        ipAddress: String? = null
    ): RefreshToken

    /**
     * 토큰 문자열로 리프레시 토큰 조회
     *
     * @param token 토큰 문자열
     * @return RefreshToken 객체 또는 null
     */
    fun findByToken(token: String): RefreshToken?

    /**
     * 리프레시 토큰 사용 시간 업데이트
     *
     * @param token 토큰 문자열
     * @return 업데이트된 RefreshToken 객체
     */
    fun updateTokenUsage(token: String): RefreshToken?

    /**
     * 리프레시 토큰 취소
     *
     * @param token 토큰 문자열
     * @return 성공 여부
     */
    fun revokeToken(token: String): Boolean

    /**
     * 사용자의 모든 리프레시 토큰 취소
     *
     * @param userId 사용자 ID
     * @return 취소된 토큰 수
     */
    fun revokeAllUserTokens(userId: Long): Int

    /**
     * 만료된 리프레시 토큰 정리
     *
     * @param before 특정 시간 이전에 만료된 토큰
     * @return 정리된 토큰 수
     */
    fun cleanupExpiredTokens(before: Instant = Instant.now()): Int

}