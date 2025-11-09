package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for finding a user by ID
 */
data class FindUserByIdCommand(
    val userId: UserId
) {
    companion object {
        fun of(userId: Long): FindUserByIdCommand {
            return FindUserByIdCommand(
                userId = UserId.from(userId)
            )
        }
    }
}