package com.stark.shoot.application.port.`in`.user.auth.command

import org.springframework.security.core.Authentication

/**
 * Command for retrieving user details
 */
data class RetrieveUserDetailsCommand(
    val authentication: Authentication?
) {
    companion object {
        fun of(authentication: Authentication?): RetrieveUserDetailsCommand {
            return RetrieveUserDetailsCommand(
                authentication = authentication
            )
        }
    }
}