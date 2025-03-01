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

        val username = jwtProvider.extractUsername(refreshToken)
        val newAccessToken = jwtProvider.generateToken(username)
        val userId = refreshTokenUseCase.getUserIdFromRefreshToken(refreshToken)

        return ResponseEntity.ok(LoginResponse(userId, newAccessToken, refreshToken))
    }

}