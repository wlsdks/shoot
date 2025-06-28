package com.stark.shoot.application.port.`in`.user.token.command

/**
 * Command for refreshing a token
 */
data class RefreshTokenCommand(
    val refreshTokenHeader: String
) {
    companion object {
        fun of(refreshTokenHeader: String): RefreshTokenCommand {
            return RefreshTokenCommand(
                refreshTokenHeader = refreshTokenHeader
            )
        }
    }
}