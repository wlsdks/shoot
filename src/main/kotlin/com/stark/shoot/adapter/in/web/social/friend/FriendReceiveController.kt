package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendReceiveController(
    private val friendReceiveUseCase: FriendReceiveUseCase
) {

    @Operation(summary = "받은 친구 요청 수락", description = "받은 친구 요청을 수락하여 서로 친구가 됨")
    @PostMapping("/accept")
    fun acceptRequest(
        @RequestParam userId: Long,
        @RequestParam requesterId: Long
    ): ResponseDto<Unit> {
        friendReceiveUseCase.acceptFriendRequest(userId, requesterId)
        return ResponseDto.success(Unit, "친구 요청을 수락했습니다.")
    }

    @Operation(summary = "받은 친구 요청 거절", description = "받은 친구 요청을 거절")
    @PostMapping("/reject")
    fun rejectRequest(
        @RequestParam userId: Long,
        @RequestParam requesterId: Long
    ): ResponseDto<Unit> {
        friendReceiveUseCase.rejectFriendRequest(userId, requesterId)
        return ResponseDto.success(Unit, "친구 요청을 거절했습니다.")
    }

}