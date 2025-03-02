package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.UpdateStatusRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.UserStatusUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
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
    private val userStatusUseCase: UserStatusUseCase,
    private val jwtProvider: JwtProvider
) {

    @Operation(
        summary = "상태 변경",
        description = """
            - 사용자의 상태를 변경합니다.
            - JWT의 sub (authentication.name)와 request.userId를 비교해 요청자가 자신만 수정할 수 있도록 보안 강화.
        """
    )
    @PutMapping("/me/status")
    fun updateStatus(
        authentication: Authentication, // 인증은 여전히 필요하지만 ID는 요청 본문에서 가져옴
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<UserResponse> {
        val token = authentication.credentials.toString()
        val jwtUsername = jwtProvider.extractUsername(token) // username 추출
        val userId = jwtProvider.extractId(token).toObjectId()
        val user = userStatusUseCase.updateStatus(userId, request.status)

        // username과 userId가 일치하는지 검증 (선택 사항)
        if (user.username != jwtUsername) {
            throw SecurityException("Unauthorized attempt to modify another user's status")
        }

        return ResponseEntity.ok(user.toResponse())
    }

}
