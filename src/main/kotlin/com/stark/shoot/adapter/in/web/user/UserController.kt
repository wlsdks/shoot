package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserController(
    private val userCreateUseCase: UserCreateUseCase,
    private val userDeleteUseCase: UserDeleteUseCase,
) {

    @Operation(
        summary = "사용자 생성",
        description = "새로운 사용자를 생성합니다."
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createUser(
        @ModelAttribute request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        val user = userCreateUseCase.createUser(request.username, request.nickname, request.password)
        return ResponseEntity.ok(user.toResponse())
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 사용자를 탈퇴 처리합니다."
    )
    @DeleteMapping("/me")
    fun deleteUser(authentication: Authentication): ResponseEntity<Void> {
        val userId = ObjectId(authentication.name) // JWT에서 추출된 userId
        userDeleteUseCase.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }

}
