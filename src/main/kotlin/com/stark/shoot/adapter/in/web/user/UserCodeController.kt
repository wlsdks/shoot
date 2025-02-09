package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.application.port.`in`.user.ManageUserCodeUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "유저 코드", description = "유저 코드 관련 API")
@RestController
@RequestMapping("/api/v1/users")
class UserCodeController(
    private val manageUserCodeUseCase: ManageUserCodeUseCase
) {

    @Operation(summary = "유저 코드 등록/수정", description = "유저가 본인의 userCode를 새로 설정 또는 수정합니다.")
    @PostMapping("/{userId}/code")
    fun updateUserCode(
        @PathVariable userId: String,
        @RequestParam code: String
    ) {
        manageUserCodeUseCase.updateUserCode(userId.toObjectId(), code)
    }

    @Operation(summary = "유저 코드 삭제", description = "유저가 본인의 userCode를 제거합니다.")
    @DeleteMapping("/{userId}/code")
    fun removeUserCode(
        @PathVariable userId: String
    ) {
        manageUserCodeUseCase.removeUserCode(userId.toObjectId())
    }

}