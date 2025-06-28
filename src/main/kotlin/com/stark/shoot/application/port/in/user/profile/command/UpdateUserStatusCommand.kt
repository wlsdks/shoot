package com.stark.shoot.application.port.`in`.user.profile.command

import com.stark.shoot.domain.user.type.UserStatus
import org.springframework.security.core.Authentication

/**
 * Command for updating a user's status
 */
data class UpdateUserStatusCommand(
    val authentication: Authentication,
    val status: UserStatus
) {
    companion object {
        fun of(authentication: Authentication, status: UserStatus): UpdateUserStatusCommand {
            return UpdateUserStatusCommand(
                authentication = authentication,
                status = status
            )
        }
    }
}