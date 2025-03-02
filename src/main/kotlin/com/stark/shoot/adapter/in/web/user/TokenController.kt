package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import com.stark.shoot.infrastructure.common.exception.InvalidRefreshTokenException
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/auth")
@RestController
class TokenController(
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val jwtProvider: JwtProvider
) {

    @Operation(
        summary = "리프레시 토큰으로 새 액세스 토큰 발급",
        description = "유효한 리프레시 토큰으로 새 액세스 토큰을 발급받음"
    )
    @PostMapping("/refresh-token")
    fun refreshToken(
        @RequestHeader("Authorization") refreshTokenHeader: String
    ): ResponseEntity<LoginResponse> {
        val refreshToken = refreshTokenHeader.replace("Bearer ", "")

        if (!jwtProvider.isTokenValid(refreshToken) || !refreshTokenUseCase.isValidRefreshToken(refreshToken)) {
            throw InvalidRefreshTokenException("유효하지 않은 리프레시 토큰입니다.")
        }

        val userId = jwtProvider.extractId(refreshToken) // sub에서 id 추출
        val username = jwtProvider.extractUsername(refreshToken) // 클레임에서 username 추출
        val newAccessToken = jwtProvider.generateToken(userId, username) // id와 username으로 새 토큰 생성
        // userId는 이미 추출했으므로 refreshTokenUseCase에서 가져올 필요 없음

        return ResponseEntity.ok(LoginResponse(userId, newAccessToken, refreshToken))
    }

}