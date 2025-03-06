package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "친구관리", description = "친구(소셜) 관련 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendController(
    private val findFriendUseCase: FindFriendUseCase,
    private val friendUseCase: FriendUseCase
) {

    @Operation(summary = "내 친구 목록 가져오기", description = "로그인 사용자의 친구 목록을 조회")
    @GetMapping
    fun getMyFriends(
        @RequestParam userId: String
    ): ResponseDto<List<FriendResponse>> {
        val friends = findFriendUseCase.getFriends(userId.toObjectId())
        return ResponseDto.success(friends)
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: String
    ): ResponseDto<List<FriendResponse>> {
        val incomingRequests = findFriendUseCase.getIncomingFriendRequests(userId.toObjectId())
        return ResponseDto.success(incomingRequests)
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: String
    ): ResponseDto<List<FriendResponse>> {
        val outgoingRequests = findFriendUseCase.getOutgoingFriendRequests(userId.toObjectId())
        return ResponseDto.success(outgoingRequests)
    }

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: String,
        @RequestParam targetUserId: String
    ): ResponseDto<Unit> {
        friendUseCase.sendFriendRequest(userId.toObjectId(), targetUserId.toObjectId())
        return ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락하여 서로 친구가 됨")
    @PostMapping("/accept")
    fun acceptRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ): ResponseDto<Unit> {
        friendUseCase.acceptFriendRequest(userId.toObjectId(), requesterId.toObjectId())
        return ResponseDto.success(Unit, "친구 요청을 수락했습니다.")
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절")
    @PostMapping("/reject")
    fun rejectRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ): ResponseDto<Unit> {
        friendUseCase.rejectFriendRequest(userId.toObjectId(), requesterId.toObjectId())
        return ResponseDto.success(Unit, "친구 요청을 거절했습니다.")
    }

    @Operation(summary = "친구 삭제", description = "친구 목록에서 사용자를 삭제합니다.")
    @DeleteMapping("/me/friends/{friendId}")
    fun removeFriend(
        authentication: Authentication,
        @PathVariable friendId: String
    ): ResponseDto<UserResponse> {
        val userId = ObjectId(authentication.name)
        val user = friendUseCase.removeFriend(userId, ObjectId(friendId))
        return ResponseDto.success(user.toResponse(), "친구가 삭제되었습니다.")
    }

}