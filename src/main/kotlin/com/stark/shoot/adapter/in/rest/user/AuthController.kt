package com.stark.shoot.adapter.`in`.rest.user

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.rest.dto.user.LoginResponse
import com.stark.shoot.adapter.`in`.rest.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.auth.UserAuthUseCase
import com.stark.shoot.application.port.`in`.user.auth.UserLoginUseCase
import com.stark.shoot.application.port.`in`.user.auth.command.LoginCommand
import com.stark.shoot.application.port.`in`.user.auth.command.RetrieveUserDetailsCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
    fun login(
        @RequestBody request: LoginRequest
    ): ResponseDto<LoginResponse> {
        val command = LoginCommand.of(request.username, request.password)
        val response = userLoginUseCase.login(command)
        return ResponseDto.success(response, "로그인에 성공했습니다.")
    }

    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인된 사용자의 전체 정보를 반환합니다."
    )
    @GetMapping("/me")
    fun getCurrentUser(
        authentication: Authentication
    ): ResponseDto<UserResponse> {
        val command = RetrieveUserDetailsCommand.of(authentication)
        val user = userAuthUseCase.retrieveUserDetails(command)
        return ResponseDto.success(user)
    }

}
