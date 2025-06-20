package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.domain.user.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendRequestController(
    private val friendRequestUseCase: FriendRequestUseCase,
) {

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: Long,
        @RequestParam targetUserId: Long
    ): ResponseDto<Unit> {
        friendRequestUseCase.sendFriendRequest(
            UserId.from(userId),
            UserId.from(targetUserId)
        )

        return ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
    }

    @Operation(summary = "보낸 친구 요청 취소하기", description = "보낸 친구 요청을 취소")
    @PostMapping("/cancel")
    fun cancelRequest(
        @RequestParam userId: Long,
        @RequestParam targetUserId: Long
    ): ResponseDto<Unit> {
        friendRequestUseCase.cancelFriendRequest(
            UserId.from(userId),
            UserId.from(targetUserId)
        )

        return ResponseDto.success(Unit, "친구 요청을 취소했습니다.")
    }

}