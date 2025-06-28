package com.stark.shoot.application.port.`in`.user.code.command

import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for updating a user's code
 */
data class UpdateUserCodeCommand(
    val userId: UserId,
    val newCode: UserCode
) {
    companion object {
        fun of(userId: Long, code: String): UpdateUserCodeCommand {
            return UpdateUserCodeCommand(
                userId = UserId.from(userId),
                newCode = UserCode.from(code)
            )
        }
    }
}