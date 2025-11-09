package com.stark.shoot.application.port.`in`.user.profile.command

import com.stark.shoot.domain.user.vo.BackgroundImageUrl
import com.stark.shoot.domain.shared.UserId

/**
 * Command for setting a user's background image
 */
data class SetBackgroundImageCommand(
    val userId: UserId,
    val backgroundImageUrl: BackgroundImageUrl
) {
    companion object {
        fun of(userId: Long, backgroundImageUrl: String): SetBackgroundImageCommand {
            return SetBackgroundImageCommand(
                userId = UserId.from(userId),
                backgroundImageUrl = BackgroundImageUrl.from(backgroundImageUrl)
            )
        }
    }
}