package com.stark.shoot.application.port.`in`.user.code.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for removing a user's code
 */
data class RemoveUserCodeCommand(
    val userId: UserId
) {
    companion object {
        fun of(userId: Long): RemoveUserCodeCommand {
            return RemoveUserCodeCommand(
                userId = UserId.from(userId)
            )
        }
    }
}