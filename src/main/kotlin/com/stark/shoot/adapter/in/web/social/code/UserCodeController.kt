package com.stark.shoot.adapter.`in`.web.social.code

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
        manageUserCodeUseCase.updateUserCode(userId.toObjectId(), code)
        return ResponseDto.success(Unit, "유저 코드가 성공적으로 설정되었습니다.")
    }

    @Operation(summary = "유저 코드로 사용자 조회", description = "상대방 코드로 사용자를 조회합니다.")
    @GetMapping("/find-by-code")
    fun findUserByCode(
        @RequestParam code: String
    ): ResponseDto<UserResponse?> {
        val user = findUserUseCase.findByUserCode(code)
        return if (user != null) {
            ResponseDto.success(user.toResponse(), "사용자를 찾았습니다.")
        } else {
            ResponseDto.success(null, "해당 코드의 사용자가 없습니다.")
        }
    }

    @Operation(summary = "유저 코드 삭제", description = "유저가 본인의 userCode를 제거합니다.")
    @DeleteMapping("/{userId}/code")
    fun removeUserCode(
        @PathVariable userId: String
    ): ResponseDto<Unit> {
        manageUserCodeUseCase.removeUserCode(userId.toObjectId())
        return ResponseDto.success(Unit, "유저 코드가 삭제되었습니다.")
    }

    @Operation(summary = "유저 코드로 친구 요청", description = "상대방 코드로 사용자를 조회한 후, 친구 요청을 보냅니다.")
    @PostMapping("/request/by-code")
    fun sendFriendRequestByCode(
        @RequestParam userId: String,
        @RequestParam targetCode: String
    ): ResponseDto<Unit> {
        // 조회는 클라이언트에서 미리 수행하는 것을 권장하지만, 여기서도 한 번 더 확인
        val targetUser = findUserUseCase.findByUserCode(targetCode)
            ?: throw ResourceNotFoundException("해당 코드($targetCode)를 가진 유저가 없습니다.")

        friendUseCase.sendFriendRequest(userId.toObjectId(), targetUser.id!!)
        return ResponseDto.success(Unit, "친구 요청을 보냈습니다.")
    }

}