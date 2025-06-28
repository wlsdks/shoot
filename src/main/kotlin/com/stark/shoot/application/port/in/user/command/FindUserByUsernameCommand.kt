package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.domain.user.vo.Username

/**
 * Command for finding a user by username
 */
data class FindUserByUsernameCommand(
    val username: Username
) {
    companion object {
        fun of(username: String): FindUserByUsernameCommand {
            return FindUserByUsernameCommand(
                username = Username.from(username)
            )
        }
    }
}