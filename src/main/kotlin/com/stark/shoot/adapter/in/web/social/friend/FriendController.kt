package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.RemoveFriendUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendController(
    private val friendRequestUseCase: FriendRequestUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase
) {

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: Long,
        @RequestParam targetUserId: Long
    ): ResponseDto<Unit> {
        friendRequestUseCase.sendFriendRequest(userId, targetUserId)
        return ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락하여 서로 친구가 됨")
    @PostMapping("/accept")
    fun acceptRequest(
        @RequestParam userId: Long,
        @RequestParam requesterId: Long
    ): ResponseDto<Unit> {
        friendRequestUseCase.acceptFriendRequest(userId, requesterId)
        return ResponseDto.success(Unit, "친구 요청을 수락했습니다.")
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절")
    @PostMapping("/reject")
    fun rejectRequest(
        @RequestParam userId: Long,
        @RequestParam requesterId: Long
    ): ResponseDto<Unit> {
        friendRequestUseCase.rejectFriendRequest(userId, requesterId)
        return ResponseDto.success(Unit, "친구 요청을 거절했습니다.")
    }

    @Operation(summary = "친구 삭제", description = "친구 목록에서 사용자를 삭제합니다.")
    @DeleteMapping("/me/friends/{friendId}")
    fun removeFriend(
        authentication: Authentication,
        @PathVariable friendId: Long
    ): ResponseDto<UserResponse> {
        val userId = authentication.name.toLong()
        val user = removeFriendUseCase.removeFriend(userId, friendId)
        return ResponseDto.success(user.toResponse(), "친구가 삭제되었습니다.")
    }

}