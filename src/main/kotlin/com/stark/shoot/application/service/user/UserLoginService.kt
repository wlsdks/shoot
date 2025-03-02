package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.UserLoginUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.application.port.out.user.token.RefreshTokenStorePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class UserLoginService(
    private val retrieveUserPort: RetrieveUserPort,
    private val refreshTokenStorePort: RefreshTokenStorePort,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) : UserLoginUseCase {

    /**
     * 로그인
     *
     * @param username 사용자명
     * @param password 비밀번호
     * @return 로그인 응답 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     */
    override fun login(
        username: String,
        password: String
    ): LoginResponse {
        // 사용자 조회
        val user: User = retrieveUserPort.findByUsername(username)
            ?: throw IllegalArgumentException("해당 username의 사용자를 찾을 수 없습니다.")

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid password")
        }

        // JWT 토큰 생성
        val accessToken = jwtProvider.generateToken(user.username, 60 * 1000) // 1분
        val refreshToken = jwtProvider.generateRefreshToken(user.username, 30 * 24 * 60) // 30일

        // MongoDB에 리프레시 토큰 저장
        refreshTokenStorePort.storeRefreshToken(user.id!!, refreshToken)
        return LoginResponse(user.id.toString(), accessToken, refreshToken)
    }

}