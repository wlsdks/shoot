package com.stark.shoot.application.port.`in`.user.block.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for blocking a user
 */
data class BlockUserCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {
    companion object {
        /**
         * Factory method to create a BlockUserCommand
         *
         * @param currentUserId The ID of the user who is blocking
         * @param targetUserId The ID of the user to be blocked
         * @return A new BlockUserCommand
         */
        fun of(currentUserId: Long, targetUserId: Long): BlockUserCommand {
            return BlockUserCommand(
                currentUserId = UserId.from(currentUserId),
                targetUserId = UserId.from(targetUserId)
            )
        }
    }
}