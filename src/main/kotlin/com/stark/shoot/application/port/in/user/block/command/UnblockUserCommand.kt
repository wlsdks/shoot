package com.stark.shoot.application.port.`in`.user.block.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for unblocking a user
 */
data class UnblockUserCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {
    companion object {
        /**
         * Factory method to create an UnblockUserCommand
         *
         * @param currentUserId The ID of the user who is unblocking
         * @param targetUserId The ID of the user to be unblocked
         * @return A new UnblockUserCommand
         */
        fun of(currentUserId: Long, targetUserId: Long): UnblockUserCommand {
            return UnblockUserCommand(
                currentUserId = UserId.from(currentUserId),
                targetUserId = UserId.from(targetUserId)
            )
        }
    }
}