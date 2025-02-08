package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.application.port.`in`.user.ManageFriendUseCase
import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendController(
    private val retrieveUserUseCase: RetrieveUserUseCase,
    private val manageFriendUseCase: ManageFriendUseCase
) {

    @Operation(summary = "내 친구 목록", description = "로그인 사용자의 친구 목록을 조회")
    @GetMapping
    fun getMyFriends(
        @RequestParam userId: String
    ): List<String> {
        val friendIds = manageFriendUseCase.getFriends(userId.toObjectId())
        return friendIds.map { it.toString() }
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: String
    ): List<String> {
        val reqs = manageFriendUseCase.getIncomingFriendRequests(userId.toObjectId())
        return reqs.map { it.toString() }
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: String
    ): List<String> {
        val reqs = manageFriendUseCase.getOutgoingFriendRequests(userId.toObjectId())
        return reqs.map { it.toString() }
    }

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청(“shoot”)을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: String,
        @RequestParam targetUserId: String
    ) {
        manageFriendUseCase.sendFriendRequest(userId.toObjectId(), targetUserId.toObjectId())
    }

    @Operation(summary = "유저 코드로 친구 요청", description = "상대방 코드로 사용자 찾은 후 친구 요청")
    @PostMapping("/request/by-code")
    fun sendFriendRequestByCode(
        @RequestParam userId: String,   // 현재 로그인 사용자
        @RequestParam targetCode: String // 상대방 userCode
    ) {
        val targetUser = retrieveUserUseCase.findByUserCode(targetCode)
            ?: throw ResourceNotFoundException("해당 코드($targetCode)를 가진 유저가 없습니다.")

        manageFriendUseCase.sendFriendRequest(userId.toObjectId(), targetUser.id!!)
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

    @Operation(summary = "친구와 채팅방 생성", description = "특정 친구와 1:1 채팅방을 만들기")
    @PostMapping("/create-chat")
    fun createChatWithFriend(
        @RequestParam userId: String,
        @RequestParam friendId: String
    ) {
        // 내부에서 createChatRoomUseCase 등을 호출해 1:1 채팅방 생성
        // 예: participants = setOf(userId, friendId)
        // (이미 존재하면 재활용하거나, 새로 만들거나...)
        // manageFriendUseCase.createChatRoom(userId, friendId) // 별도 UseCase 구현도 가능
    }

}