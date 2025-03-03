package com.stark.shoot.application.service.user.token

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import com.stark.shoot.application.port.out.user.token.RefreshTokenStorePort
import com.stark.shoot.infrastructure.common.exception.web.InvalidRefreshTokenException
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    private val refreshTokenStorePort: RefreshTokenStorePort,
    private val jwtProvider: JwtProvider
) : RefreshTokenUseCase {

    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급
     *
     * @param refreshTokenHeader 리프레시 토큰 헤더
     * @return 새 액세스 토큰
     */
    override fun generateNewAccessToken(
        refreshTokenHeader: String
    ): LoginResponse {
        // 헤더에서 리프레시 토큰 추출
        val refreshToken = refreshTokenHeader.replace("Bearer ", "")

        // 리프레시 토큰 유효성 검증
        validationRefreshToken(refreshToken)

        // 리프레시 토큰에서 userId와 username 추출
        val userId = jwtProvider.extractId(refreshToken)                 // sub에서 id 추출
        val username = jwtProvider.extractUsername(refreshToken)         // 클레임에서 username 추출
        val newAccessToken = jwtProvider.generateToken(userId, username) // id와 username으로 새 토큰 생성

        // 새 액세스 토큰 생성 후 리프레시 토큰 반환
        return LoginResponse(userId, newAccessToken, refreshToken)
    }


    /**
     * 리프레시 토큰 유효성 검증
     *
     * @param refreshToken 리프레시 토큰
     */
    private fun validationRefreshToken(refreshToken: String) {
        // 토큰 검증 (헤더로 받은 토큰을 먼저 유틸로 검증)
        val isValidRefreshToken = !jwtProvider.isTokenValid(refreshToken)

        // 토큰 검증 (저장된 리프레시 토큰을 확인해서 2차 검증)
        val isValidRefreshTokenStoredAt = !refreshTokenStorePort.isValidRefreshToken(refreshToken)

        // 2가지 유효성 검증을 통과하지 못하면 예외 발생
        if (isValidRefreshToken || isValidRefreshTokenStoredAt) {
            throw InvalidRefreshTokenException("유효하지 않은 리프레시 토큰입니다.")
        }
    }

}