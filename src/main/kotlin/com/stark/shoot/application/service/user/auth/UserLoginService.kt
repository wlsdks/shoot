package com.stark.shoot.application.service.user.auth

import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.auth.UserLoginUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.token.RefreshTokenStorePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class UserLoginService(
    private val findUserPort: FindUserPort,
    private val refreshTokenStorePort: RefreshTokenStorePort,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) : UserLoginUseCase {

    /**
     * 사용자 로그인
     *
     * @param request 로그인 요청 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     * @return 로그인 응답 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     */
    override fun login(
        request: LoginRequest
    ): LoginResponse {
        // 사용자 조회
        val user: User = findUserPort.findByUsername(request.username)
            ?: throw IllegalArgumentException("해당 username의 사용자를 찾을 수 없습니다.")

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid password")
        }

        // JWT 토큰 생성
        val accessToken = jwtProvider.generateToken(user.id.toString(), user.username)
        val refreshToken = jwtProvider.generateRefreshToken(user.id.toString(), user.username, 43200) // 30일

        // MongoDB에 리프레시 토큰 저장 (todo: 로그인 할때마다 하는게 맞나? 확인 필요)
        refreshTokenStorePort.storeRefreshToken(user.id!!, refreshToken)
        return LoginResponse(user.id.toString(), accessToken, refreshToken)
    }

}