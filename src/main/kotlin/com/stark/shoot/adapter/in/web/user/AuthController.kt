package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.auth.UserAuthUseCase
import com.stark.shoot.application.port.`in`.user.auth.UserLoginUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "인증", description = "사용자 인증 관련 API")
@RequestMapping("/api/v1/auth")
@RestController
class AuthController(
    private val userLoginUseCase: UserLoginUseCase,
    private val userAuthUseCase: UserAuthUseCase
) {

    @Operation(
        summary = "사용자 로그인",
        description = "username, password로 로그인 후 JWT 토큰 발급"
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseDto<LoginResponse> {
        return try {
            val response = userLoginUseCase.login(request)
            ResponseDto.success(response, "로그인에 성공했습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "로그인에 실패했습니다: ${e.message}",
                ApiException.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED,
                e
            )
        }
    }

    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인된 사용자의 전체 정보를 반환합니다."
    )
    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseDto<UserResponse> {
        return try {
            val user = userAuthUseCase.retrieveUserDetails(authentication)
            ResponseDto.success(user)
        } catch (e: Exception) {
            throw ApiException(
                "사용자 정보 조회에 실패했습니다: ${e.message}",
                ApiException.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED,
                e
            )
        }
    }

}