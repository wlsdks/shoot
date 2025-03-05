package com.stark.shoot.adapter.`in`.web.social.friend

import com.stark.shoot.adapter.`in`.web.dto.ApiException
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
import org.springframework.http.HttpStatus
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
        return try {
            val friends = findFriendUseCase.getFriends(userId.toObjectId())
            ResponseDto.success(friends)
        } catch (e: Exception) {
            throw ApiException(
                "친구 목록 조회에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                e
            )
        }
    }

    @Operation(summary = "받은 친구 요청 목록", description = "내가 받은 친구 요청들(incoming)")
    @GetMapping("/incoming")
    fun getIncomingFriendRequests(
        @RequestParam userId: String
    ): ResponseDto<List<FriendResponse>> {
        return try {
            val incomingRequests = findFriendUseCase.getIncomingFriendRequests(userId.toObjectId())
            ResponseDto.success(incomingRequests)
        } catch (e: Exception) {
            throw ApiException(
                "받은 친구 요청 목록 조회에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                e
            )
        }
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 보낸 친구 요청들(outgoing)")
    @GetMapping("/outgoing")
    fun getOutgoingFriendRequests(
        @RequestParam userId: String
    ): ResponseDto<List<FriendResponse>> {
        return try {
            val outgoingRequests = findFriendUseCase.getOutgoingFriendRequests(userId.toObjectId())
            ResponseDto.success(outgoingRequests)
        } catch (e: Exception) {
            throw ApiException(
                "보낸 친구 요청 목록 조회에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                e
            )
        }
    }

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냄")
    @PostMapping("/request")
    fun sendFriendRequest(
        @RequestParam userId: String,
        @RequestParam targetUserId: String
    ): ResponseDto<Unit> {
        return try {
            friendUseCase.sendFriendRequest(userId.toObjectId(), targetUserId.toObjectId())
            ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
        } catch (e: Exception) {
            when {
                e.message?.contains("자기 자신") == true -> throw ApiException(
                    e.message!!,
                    ApiException.SELF_FRIEND_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    e
                )

                e.message?.contains("이미 친구") == true -> throw ApiException(
                    e.message!!,
                    ApiException.ALREADY_FRIENDS,
                    HttpStatus.BAD_REQUEST,
                    e
                )

                e.message?.contains("이미 친구 요청") == true -> throw ApiException(
                    e.message!!,
                    ApiException.FRIEND_REQUEST_ALREADY_SENT,
                    HttpStatus.BAD_REQUEST,
                    e
                )

                else -> throw ApiException(
                    "친구 요청에 실패했습니다: ${e.message}",
                    ApiException.INVALID_INPUT,
                    HttpStatus.BAD_REQUEST,
                    e
                )
            }
        }
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락하여 서로 친구가 됨")
    @PostMapping("/accept")
    fun acceptRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ): ResponseDto<Unit> {
        return try {
            friendUseCase.acceptFriendRequest(userId.toObjectId(), requesterId.toObjectId())
            ResponseDto.success(Unit, "친구 요청을 수락했습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "친구 요청 수락에 실패했습니다: ${e.message}",
                ApiException.FRIEND_REQUEST_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절")
    @PostMapping("/reject")
    fun rejectRequest(
        @RequestParam userId: String,
        @RequestParam requesterId: String
    ): ResponseDto<Unit> {
        return try {
            friendUseCase.rejectFriendRequest(userId.toObjectId(), requesterId.toObjectId())
            ResponseDto.success(Unit, "친구 요청을 거절했습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "친구 요청 거절에 실패했습니다: ${e.message}",
                ApiException.FRIEND_REQUEST_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "친구 삭제", description = "친구 목록에서 사용자를 삭제합니다.")
    @DeleteMapping("/me/friends/{friendId}")
    fun removeFriend(
        authentication: Authentication,
        @PathVariable friendId: String
    ): ResponseDto<UserResponse> {
        return try {
            val userId = ObjectId(authentication.name)
            val user = friendUseCase.removeFriend(userId, ObjectId(friendId))
            ResponseDto.success(user.toResponse(), "친구가 삭제되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "친구 삭제에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}