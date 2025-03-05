package com.stark.shoot.adapter.`in`.web.social.code

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendUseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "유저 코드", description = "유저 코드 관련 API")
@RestController
@RequestMapping("/api/v1/users")
class UserCodeController(
    private val findUserUseCase: FindUserUseCase,
    private val manageUserCodeUseCase: ManageUserCodeUseCase,
    private val friendUseCase: FriendUseCase
) {

    @Operation(summary = "유저 코드 등록/수정", description = "유저가 본인의 userCode를 새로 설정 또는 수정합니다.")
    @PostMapping("/{userId}/code")
    fun updateUserCode(
        @PathVariable userId: String,
        @RequestParam code: String
    ): ResponseDto<Unit> {
        return try {
            manageUserCodeUseCase.updateUserCode(userId.toObjectId(), code)
            ResponseDto.success(Unit, "유저 코드가 성공적으로 설정되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "유저 코드 설정에 실패했습니다: ${e.message}",
                ApiException.DUPLICATE_RESOURCE,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "유저 코드로 사용자 조회", description = "상대방 코드로 사용자를 조회합니다.")
    @GetMapping("/find-by-code")
    fun findUserByCode(
        @RequestParam code: String
    ): ResponseDto<UserResponse?> {
        return try {
            val user = findUserUseCase.findByUserCode(code)
            if (user != null) {
                ResponseDto.success(user.toResponse(), "사용자를 찾았습니다.")
            } else {
                ResponseDto.success(null, "해당 코드의 사용자가 없습니다.")
            }
        } catch (e: Exception) {
            throw ApiException(
                "사용자 조회에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                e
            )
        }
    }

    @Operation(summary = "유저 코드 삭제", description = "유저가 본인의 userCode를 제거합니다.")
    @DeleteMapping("/{userId}/code")
    fun removeUserCode(
        @PathVariable userId: String
    ): ResponseDto<Unit> {
        return try {
            manageUserCodeUseCase.removeUserCode(userId.toObjectId())
            ResponseDto.success(Unit, "유저 코드가 삭제되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "유저 코드 삭제에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

    @Operation(summary = "유저 코드로 친구 요청", description = "상대방 코드로 사용자를 조회한 후, 친구 요청을 보냅니다.")
    @PostMapping("/request/by-code")
    fun sendFriendRequestByCode(
        @RequestParam userId: String,
        @RequestParam targetCode: String
    ): ResponseDto<Unit> {
        return try {
            // 조회는 클라이언트에서 미리 수행하는 것을 권장하지만, 여기서도 한 번 더 확인
            val targetUser = findUserUseCase.findByUserCode(targetCode)
                ?: throw ResourceNotFoundException("해당 코드($targetCode)를 가진 유저가 없습니다.")

            friendUseCase.sendFriendRequest(userId.toObjectId(), targetUser.id!!)
            ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
        } catch (e: Exception) {
            when {
                e is ResourceNotFoundException -> throw ApiException(
                    e.message ?: "사용자를 찾을 수 없습니다",
                    ApiException.RESOURCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
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
                    "친구 요청 실패: ${e.message}",
                    ApiException.INVALID_INPUT,
                    HttpStatus.BAD_REQUEST,
                    e
                )
            }
        }
    }

}