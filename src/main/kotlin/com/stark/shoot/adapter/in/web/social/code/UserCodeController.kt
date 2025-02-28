package com.stark.shoot.adapter.`in`.web.social.code

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.ManageFriendUseCase
import com.stark.shoot.application.port.`in`.user.ManageUserCodeUseCase
import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "유저 코드", description = "유저 코드 관련 API")
@RestController
@RequestMapping("/api/v1/users")
class UserCodeController(
    private val retrieveUserUseCase: RetrieveUserUseCase,
    private val manageUserCodeUseCase: ManageUserCodeUseCase,
    private val manageFriendUseCase: ManageFriendUseCase
) {

    @Operation(summary = "유저 코드 등록/수정", description = "유저가 본인의 userCode를 새로 설정 또는 수정합니다.")
    @PostMapping("/{userId}/code")
    fun updateUserCode(
        @PathVariable userId: String,
        @RequestParam code: String
    ) {
        manageUserCodeUseCase.updateUserCode(userId.toObjectId(), code)
    }

    @Operation(summary = "유저 코드로 사용자 조회", description = "상대방 코드로 사용자를 조회합니다.")
    @GetMapping("/find-by-code")
    fun findUserByCode(
        @RequestParam code: String
    ): ResponseEntity<UserResponse?> {
        val user = retrieveUserUseCase.findByUserCode(code) ?: return ResponseEntity.ok(null)

        // 검색 결과가 없으면 404 대신 200 OK와 null을 반환함
        val response = UserResponse(
            id = user.id.toString(),
            username = user.username,
            nickname = user.nickname,
            userCode = user.userCode
        )
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "유저 코드 삭제", description = "유저가 본인의 userCode를 제거합니다.")
    @DeleteMapping("/{userId}/code")
    fun removeUserCode(
        @PathVariable userId: String
    ) {
        manageUserCodeUseCase.removeUserCode(userId.toObjectId())
    }

    @Operation(summary = "유저 코드로 친구 요청", description = "상대방 코드로 사용자를 조회한 후, 친구 요청을 보냅니다.")
    @PostMapping("/request/by-code")
    fun sendFriendRequestByCode(
        @RequestParam userId: String,   // 현재 로그인 사용자
        @RequestParam targetCode: String // 상대방 userCode
    ) {
        // 조회는 클라이언트에서 미리 수행하는 것을 권장하지만, 여기서도 한 번 더 확인
        val targetUser = retrieveUserUseCase.findByUserCode(targetCode)
            ?: throw ResourceNotFoundException("해당 코드($targetCode)를 가진 유저가 없습니다.")

        manageFriendUseCase.sendFriendRequest(userId.toObjectId(), targetUser.id!!)
    }

}