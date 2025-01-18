package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Service

@Service
class UserCreateService(
    private val userCreatePort: UserCreatePort
) : UserCreateUseCase {

    override fun createUser(username: String, nickname: String): User {
        val user = User(
            username = username,
            nickname = nickname
        )

        return userCreatePort.createUser(user)
    }

}