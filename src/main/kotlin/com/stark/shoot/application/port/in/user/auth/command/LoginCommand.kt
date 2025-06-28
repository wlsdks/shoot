package com.stark.shoot.application.port.`in`.user.auth.command

import com.stark.shoot.domain.user.vo.Username

/**
 * Command for user login
 */
data class LoginCommand(
    val username: Username,
    val password: String
) {
    companion object {
        fun of(username: String, password: String): LoginCommand {
            return LoginCommand(
                username = Username.from(username),
                password = password
            )
        }
    }
}