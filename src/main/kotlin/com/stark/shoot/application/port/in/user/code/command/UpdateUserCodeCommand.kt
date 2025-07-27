package com.stark.shoot.application.port.`in`.user.code.command

import com.stark.shoot.adapter.`in`.rest.dto.social.UpdateUserCodeRequest
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId

data class UpdateUserCodeCommand(
    val userId: UserId,
    val newCode: UserCode
) {

    companion object {
        fun of(request: UpdateUserCodeRequest): UpdateUserCodeCommand {
            return UpdateUserCodeCommand(
                userId = UserId.from(request.userId),
                newCode = UserCode.from(request.code)
            )
        }
    }

}