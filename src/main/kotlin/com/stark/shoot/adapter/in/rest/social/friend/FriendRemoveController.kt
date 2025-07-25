package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.rest.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendRemoveController(
    private val friendRemoveUseCase: FriendRemoveUseCase
) {

    @Operation(summary = "친구 삭제", description = "친구 목록에서 사용자를 삭제합니다.")
    @DeleteMapping("/me/friends/{friendId}")
    fun removeFriend(
        authentication: Authentication,
        @PathVariable friendId: Long
    ): ResponseDto<UserResponse> {
        val command = RemoveFriendCommand.of(authentication, friendId)
        val user = friendRemoveUseCase.removeFriend(command)
        return ResponseDto.success(user.toResponse(), "친구가 삭제되었습니다.")
    }

}
