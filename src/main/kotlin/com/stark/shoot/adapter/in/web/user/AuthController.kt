package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginRequest
import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.UserLoginUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/auth")
@RestController
class AuthController(
    private val userLoginUseCase: UserLoginUseCase
) {

    @Operation(
        summary = "사용자 로그인",
        description = "username, password로 로그인 후 JWT 토큰 발급"
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val token = userLoginUseCase.login(request.username, request.password)

        // 예시: token 발급 후, userId도 함께 반환한다고 가정
        // userId를 username 대신 저장했으면, username으로 응답할 수도 있습니다.
        val response = LoginResponse(
            userId = request.username,
            accessToken = token
        )

        return ResponseEntity.ok(response)
    }

}