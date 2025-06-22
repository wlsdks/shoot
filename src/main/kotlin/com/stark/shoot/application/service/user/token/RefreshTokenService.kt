package com.stark.shoot.application.service.user.token

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import com.stark.shoot.application.port.out.user.token.RefreshTokenCommandPort
import com.stark.shoot.application.port.out.user.token.RefreshTokenQueryPort
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import com.stark.shoot.infrastructure.exception.web.InvalidRefreshTokenException
import java.time.Instant

@UseCase
class RefreshTokenService(
    private val refreshTokenCommandPort: RefreshTokenCommandPort,
    private val refreshTokenQueryPort: RefreshTokenQueryPort,
    private val jwtProvider: JwtProvider
) : RefreshTokenUseCase {

    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급
     *
     * @param refreshTokenHeader 리프레시 토큰 헤더
     * @return 새 액세스 토큰 및 리프레시 토큰 정보
     */
    override fun generateNewAccessToken(
        refreshTokenHeader: String
    ): LoginResponse {
        // 헤더에서 리프레시 토큰 추출
        val refreshToken = extractTokenFromHeader(refreshTokenHeader)

        val tokenValue = RefreshTokenValue.from(refreshToken)

        // 리프레시 토큰 유효성 검증
        validateRefreshToken(tokenValue)

        // 리프레시 토큰 사용 기록 업데이트
        refreshTokenCommandPort.updateTokenUsage(tokenValue)

        // 리프레시 토큰에서 userId와 username 추출
        val userId = jwtProvider.extractId(refreshToken)
        val username = jwtProvider.extractUsername(refreshToken)

        // 새 액세스 토큰 생성
        val newAccessToken = jwtProvider.generateToken(userId, username)

        // 응답 생성 (리프레시 토큰은 재사용)
        return LoginResponse(userId, newAccessToken, refreshToken)
    }

    /**
     * 헤더에서 토큰 추출
     */
    private fun extractTokenFromHeader(header: String): String {
        return if (header.startsWith("Bearer ")) {
            header.substring(7)
        } else {
            header // 이미 토큰만 전달된 경우
        }
    }

    /**
     * 리프레시 토큰 유효성 검증
     */
    private fun validateRefreshToken(refreshToken: RefreshTokenValue) {
        // 토큰 형식 및 서명 검증
        if (!jwtProvider.isRefreshTokenValid(refreshToken)) {
            throw InvalidRefreshTokenException("유효하지 않은 리프레시 토큰 형식입니다.")
        }

        // DB에 저장된 토큰 확인
        val storedToken = refreshTokenQueryPort.findByToken(refreshToken)
            ?: throw InvalidRefreshTokenException("존재하지 않는 리프레시 토큰입니다.")

        // 토큰 상태 확인 (취소 여부)
        if (storedToken.isRevoked) {
            throw InvalidRefreshTokenException("취소된 리프레시 토큰입니다.")
        }

        // 토큰 만료 확인
        if (storedToken.expirationDate.isBefore(Instant.now())) {
            throw InvalidRefreshTokenException("만료된 리프레시 토큰입니다.")
        }
    }

}