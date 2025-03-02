package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.UpdateStatusRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.UserStatusUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserStatusController(
    private val userStatusUseCase: UserStatusUseCase
) {

    @Operation(
        summary = "상태 변경",
        description = "사용자의 상태를 변경합니다."
    )
    @PutMapping("/me/status")
    fun updateStatus(
        authentication: Authentication,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<UserResponse> {
        val userId = ObjectId(authentication.name)
        val user = userStatusUseCase.updateStatus(userId, request.status)
        return ResponseEntity.ok(user.toResponse())
    }

}
