package com.stark.shoot.application.port.`in`.user.profile.command

import com.stark.shoot.domain.user.vo.*

/**
 * Command for updating a user's profile
 */
data class UpdateProfileCommand(
    val userId: UserId,
    val nickname: Nickname?,
    val profileImageUrl: ProfileImageUrl?,
    val backgroundImageUrl: BackgroundImageUrl?,
    val bio: UserBio?
) {
    companion object {
        fun of(
            userId: Long,
            nickname: String?,
            profileImageUrl: String?,
            backgroundImageUrl: String?,
            bio: String?
        ): UpdateProfileCommand {
            return UpdateProfileCommand(
                userId = UserId.from(userId),
                nickname = nickname?.let { Nickname.from(it) },
                profileImageUrl = profileImageUrl?.let { ProfileImageUrl.from(it) },
                backgroundImageUrl = backgroundImageUrl?.let { BackgroundImageUrl.from(it) },
                bio = bio?.let { UserBio.from(it) }
            )
        }
    }
}