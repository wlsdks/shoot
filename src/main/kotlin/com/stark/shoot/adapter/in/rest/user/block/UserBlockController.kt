package com.stark.shoot.adapter.`in`.rest.user.block

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.application.port.`in`.user.block.UserBlockUseCase
import com.stark.shoot.application.port.`in`.user.block.command.BlockUserCommand
import com.stark.shoot.application.port.`in`.user.block.command.UnblockUserCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자 차단", description = "사용자 차단 관련 API")
@RestController
@RequestMapping("/api/v1/users")
class UserBlockController(
    private val userBlockUseCase: UserBlockUseCase
) {

    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다.")
    @PostMapping("/{targetId}/block")
    fun blockUser(
        @PathVariable targetId: Long,
        authentication: Authentication
    ): ResponseDto<Boolean> {
        val userId = authentication.name.toLong()
        val command = BlockUserCommand.of(userId, targetId)
        userBlockUseCase.blockUser(command)
        return ResponseDto.success(true, "사용자를 차단했습니다.")
    }

    @Operation(summary = "차단 해제", description = "차단한 사용자를 해제합니다.")
    @DeleteMapping("/{targetId}/block")
    fun unblockUser(
        @PathVariable targetId: Long,
        authentication: Authentication
    ): ResponseDto<Boolean> {
        val userId = authentication.name.toLong()
        val command = UnblockUserCommand.of(userId, targetId)
        userBlockUseCase.unblockUser(command)
        return ResponseDto.success(true, "차단을 해제했습니다.")
    }
}
