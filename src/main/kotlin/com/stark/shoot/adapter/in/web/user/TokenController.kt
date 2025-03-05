package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.token.RefreshTokenUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "JWT 토큰(Access) 재발급", description = "JWT 토큰 재발급 관련 API")
@RequestMapping("/api/v1/auth")
@RestController
class TokenController(
    private val refreshTokenUseCase: RefreshTokenUseCase
) {

    @Operation(
        summary = "리프레시 토큰으로 새 액세스 토큰 발급",
        description = "유효한 리프레시 토큰으로 새 액세스 토큰을 발급받음"
    )
    @PostMapping("/refresh-token")
    fun refreshToken(
        @RequestHeader("Authorization") refreshTokenHeader: String
    ): ResponseDto<LoginResponse> {
        return try {
            val loginResponse = refreshTokenUseCase.generateNewAccessToken(refreshTokenHeader)
            ResponseDto.success(loginResponse, "새 액세스 토큰이 발급되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "토큰 갱신에 실패했습니다: ${e.message}",
                ApiException.TOKEN_EXPIRED,
                HttpStatus.UNAUTHORIZED,
                e
            )
        }
    }

}