package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserController(
    private val userCreateUseCase: UserCreateUseCase,
) {

    @Operation(
        summary = "사용자 생성",
        description = "사용자를 생성합니다."
    )
    @PostMapping
    fun createUser(
        @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        val user = userCreateUseCase.createUser(request.username, request.nickname)
        val userResponse = UserResponse(user.id.toString(), user.username, user.nickname)
        return ResponseEntity.ok(userResponse)
    }

}
