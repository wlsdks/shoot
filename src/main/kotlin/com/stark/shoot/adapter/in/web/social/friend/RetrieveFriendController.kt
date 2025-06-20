package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.domain.user.vo.UserId
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
        val friends = findFriendUseCase.getFriends(UserId.from(userId))
        return ResponseDto.success(friends)
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: Long
    ): ResponseDto<List<FriendResponse>> {
        val incomingRequests = findFriendUseCase.getIncomingFriendRequests(UserId.from(userId))
        return ResponseDto.success(incomingRequests)
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: Long
    ): ResponseDto<List<FriendResponse>> {
        val outgoingRequests = findFriendUseCase.getOutgoingFriendRequests(UserId.from(userId))
        return ResponseDto.success(outgoingRequests)
    }

}