package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.adapter.`in`.rest.dto.user.CreateUserRequest
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.UserBio
import com.stark.shoot.domain.user.vo.Username
import org.springframework.web.multipart.MultipartFile

data class CreateUserCommand(
    val username: Username,
    val nickname: Nickname,
    val password: String,
    val email: String,
    val bio: UserBio?,
    val profileImage: MultipartFile?
) {

    companion object {
        fun of(request: CreateUserRequest): CreateUserCommand {
            return CreateUserCommand(
                username = Username.from(request.username),
                nickname = Nickname.from(request.nickname),
                password = request.password,
                email = request.email,
                bio = request.bio?.let { UserBio.from(it) },
                profileImage = request.profileImage
            )
        }
    }

}