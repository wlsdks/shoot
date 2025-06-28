package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.UserBio
import com.stark.shoot.domain.user.vo.Username
import org.springframework.web.multipart.MultipartFile

/**
 * Command for creating a user
 */
data class CreateUserCommand(
    val username: Username,
    val nickname: Nickname,
    val password: String,
    val email: String,
    val bio: UserBio?,
    val profileImage: MultipartFile?
) {
    companion object {
        fun of(
            username: String,
            nickname: String,
            password: String,
            email: String,
            bio: String? = null,
            profileImage: MultipartFile? = null
        ): CreateUserCommand {
            return CreateUserCommand(
                username = Username.from(username),
                nickname = Nickname.from(nickname),
                password = password,
                email = email,
                bio = bio?.let { UserBio.from(it) },
                profileImage = profileImage
            )
        }
    }
}