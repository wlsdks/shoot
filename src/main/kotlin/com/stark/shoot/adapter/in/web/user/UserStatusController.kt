package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.UpdateStatusRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.profile.UserStatusUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
        summary = "사용자 상태 변경",
        description = """
            - 사용자의 상태를 변경합니다.
            - 토큰에 포함된 사용자 ID를 기반으로 상태를 변경합니다.
            - 사용자 본인이 맞을때만 상태가 변경됩니다.
        """
    )
    @PutMapping("/me/status")
    fun updateStatus(
        authentication: Authentication, // 인증은 여전히 필요하지만 ID는 요청 본문에서 가져옴
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<UserResponse> {
        val user = userStatusUseCase.updateStatus(authentication, request.status)
        return ResponseEntity.ok(user.toResponse())
    }

}
