package com.stark.shoot.application.port.`in`.user.profile.command

import com.stark.shoot.domain.user.vo.ProfileImageUrl
import com.stark.shoot.domain.shared.UserId

/**
 * Command for setting a user's profile image
 */
data class SetProfileImageCommand(
    val userId: UserId,
    val profileImageUrl: ProfileImageUrl
) {
    companion object {
        fun of(userId: Long, profileImageUrl: String): SetProfileImageCommand {
            return SetProfileImageCommand(
                userId = UserId.from(userId),
                profileImageUrl = ProfileImageUrl.from(profileImageUrl)
            )
        }
    }
}