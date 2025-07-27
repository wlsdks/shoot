package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.social.friend.CancelFriendRequest
import com.stark.shoot.adapter.`in`.rest.dto.social.friend.SendFriendRequest
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.CancelFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendRequestController(
    private val friendRequestUseCase: FriendRequestUseCase,
) {

    @Operation(
        summary = "친구 요청 보내기",
        description = "다른 사용자에게 친구 요청을 보냄"
    )
    @PostMapping("/request")
    fun sendFriendRequest(@RequestBody request: SendFriendRequest): ResponseDto<Unit> {
        val command = SendFriendRequestCommand.of(request)
        friendRequestUseCase.sendFriendRequest(command)
        return ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
    }

    @Operation(
        summary = "보낸 친구 요청 취소하기",
        description = "보낸 친구 요청을 취소"
    )
    @PostMapping("/cancel")
    fun cancelRequest(@RequestBody request: CancelFriendRequest): ResponseDto<Unit> {
        val command = CancelFriendRequestCommand.of(request)
        friendRequestUseCase.cancelFriendRequest(command)
        return ResponseDto.success(Unit, "친구 요청을 취소했습니다.")
    }

}
