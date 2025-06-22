package com.stark.shoot.application.service.user.auth

import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.auth.UserLoginUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.token.RefreshTokenCommandPort
import com.stark.shoot.domain.user.vo.Username
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserLoginService(
    private val findUserPort: FindUserPort,
    private val refreshTokenCommandPort: RefreshTokenCommandPort,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) : UserLoginUseCase {

    /**
     * 사용자 로그인
     *
     * @param request 로그인 요청 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     * @return 로그인 응답 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     */
    override fun login(request: LoginRequest): LoginResponse {
        // 사용자 조회
        val user = findUserPort.findByUsername(Username.from(request.username))
            ?: throw IllegalArgumentException("해당 username의 사용자를 찾을 수 없습니다.")

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid password")
        }

        // JWT 토큰 생성
        val userId = user.id ?: throw IllegalStateException("User ID must not be null")

        val accessToken = jwtProvider.generateToken(userId.toString(), user.username.value)
        val refreshToken = jwtProvider.generateRefreshToken(userId.toString(), user.username.value, 43200) // 30일

        // 리프레시 토큰 저장 (기본 정보만)
        refreshTokenCommandPort.createRefreshToken(
            userId = userId,
            token = refreshToken
        )

        return LoginResponse(userId.toString(), accessToken, refreshToken.value)
    }

}