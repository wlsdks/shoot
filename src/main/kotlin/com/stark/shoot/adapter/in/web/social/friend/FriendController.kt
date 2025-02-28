package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.ManageFriendUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendController(
    private val manageFriendUseCase: ManageFriendUseCase
) {

    @Operation(summary = "내 친구 목록 가져오기", description = "로그인 사용자의 친구 목록을 조회")
    @GetMapping
    fun getMyFriends(
        @RequestParam userId: String
    ): List<FriendResponse> {
        return manageFriendUseCase.getFriends(userId.toObjectId());
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: String
    ): List<FriendResponse> {
        return manageFriendUseCase.getIncomingFriendRequests(userId.toObjectId())
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: String
    ): List<FriendResponse> {
        return manageFriendUseCase.getOutgoingFriendRequests(userId.toObjectId())
    }

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청(“shoot”)을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: String,
        @RequestParam targetUserId: String
    ) {
        manageFriendUseCase.sendFriendRequest(userId.toObjectId(), targetUserId.toObjectId())
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락하여 서로 친구가 됨")
    @PostMapping("/accept")
    fun acceptRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ) {
        manageFriendUseCase.acceptFriendRequest(userId.toObjectId(), requesterId.toObjectId())
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절")
    @PostMapping("/reject")
    fun rejectRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ) {
        manageFriendUseCase.rejectFriendRequest(userId.toObjectId(), requesterId.toObjectId())
    }

}