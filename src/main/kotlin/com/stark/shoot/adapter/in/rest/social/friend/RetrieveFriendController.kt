package com.stark.shoot.adapter.`in`.rest.social.friend

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetFriendsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetIncomingFriendRequestsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetOutgoingFriendRequestsCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class RetrieveFriendController(
    private val findFriendUseCase: FindFriendUseCase,
) {

    @Operation(summary = "내 친구 목록 가져오기", description = "로그인 사용자의 친구 목록을 조회")
    @GetMapping
    fun getMyFriends(
        @RequestParam userId: Long
    ): ResponseDto<List<FriendResponse>> {
        val command = GetFriendsCommand.of(userId)
        val friends = findFriendUseCase.getFriends(command)
        return ResponseDto.success(friends)
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: Long
    ): ResponseDto<List<FriendResponse>> {
        val command = GetIncomingFriendRequestsCommand.of(userId)
        val incomingRequests = findFriendUseCase.getIncomingFriendRequests(command)
        return ResponseDto.success(incomingRequests)
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: Long
    ): ResponseDto<List<FriendResponse>> {
        val command = GetOutgoingFriendRequestsCommand.of(userId)
        val outgoingRequests = findFriendUseCase.getOutgoingFriendRequests(command)
        return ResponseDto.success(outgoingRequests)
    }

}
