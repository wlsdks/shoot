package com.stark.shoot.application.service.user.auth

import com.stark.shoot.adapter.`in`.rest.dto.user.login.LoginResponse
import com.stark.shoot.application.port.`in`.user.auth.UserLoginUseCase
import com.stark.shoot.application.port.`in`.user.auth.command.LoginCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.token.RefreshTokenCommandPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import com.stark.shoot.infrastructure.exception.UserException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserLoginService(
    private val userQueryPort: UserQueryPort,
    private val refreshTokenCommandPort: RefreshTokenCommandPort,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) : UserLoginUseCase {

    /**
     * 사용자 로그인
     *
     * @param command 로그인 커맨드 (사용자명, 비밀번호)
     * @return 로그인 응답 (사용자 ID, 액세스 토큰, 리프레시 토큰)
     */
    override fun login(command: LoginCommand): LoginResponse {
        // 사용자 조회
        val user = userQueryPort.findByUsername(command.username)
            ?: throw UserException.InvalidCredentials("해당 username의 사용자를 찾을 수 없습니다.")

        // 비밀번호 검증
        if (!passwordEncoder.matches(command.password, user.passwordHash)) {
            throw UserException.InvalidCredentials("비밀번호가 일치하지 않습니다.")
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
