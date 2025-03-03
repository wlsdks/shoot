package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.UserAuthUseCase
import com.stark.shoot.application.port.`in`.user.UserLoginUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@Tag(name = "인증", description = "사용자 인증 관련 API")
@RequestMapping("/api/v1/auth")
@RestController
class AuthController(
    private val userLoginUseCase: UserLoginUseCase,
    private val userAuthUseCase: UserAuthUseCase,
) {

    @Operation(
        summary = "사용자 로그인",
        description = "username, password로 로그인 후 JWT 토큰 발급"
    )
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {
        val response = userLoginUseCase.login(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인된 사용자의 전체 정보를 반환합니다."
    )
    @GetMapping("/me")
    fun getCurrentUser(
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val user = userAuthUseCase.retrieveUserDetails(authentication)
        return ResponseEntity.ok(user)
    }

}