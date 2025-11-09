package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for creating an SSE emitter for a user
 */
data class CreateEmitterCommand(
    val userId: UserId
) {
    companion object {
        fun of(userId: UserId): CreateEmitterCommand {
            return CreateEmitterCommand(userId)
        }
        
        fun of(userId: Long): CreateEmitterCommand {
            return CreateEmitterCommand(UserId.from(userId))
        }
    }
}