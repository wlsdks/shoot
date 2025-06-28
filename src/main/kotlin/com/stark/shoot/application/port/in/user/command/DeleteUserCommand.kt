package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for deleting a user
 */
data class DeleteUserCommand(
    val userId: UserId
) {
    companion object {
        fun of(userId: Long): DeleteUserCommand {
            return DeleteUserCommand(
                userId = UserId.from(userId)
            )
        }
    }
}