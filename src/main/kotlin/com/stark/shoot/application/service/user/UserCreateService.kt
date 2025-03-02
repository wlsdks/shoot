package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserCreateService(
    private val userCreatePort: UserCreatePort
) : UserCreateUseCase {

    // 예시: User 생성 시 자동으로 userCode 생성
    override fun createUser(username: String, nickname: String): User {
        val generatedUserCode = UUID.randomUUID().toString() // 또는 원하는 로직

        val user = User(
            username = username,
            nickname = nickname,
            userCode = generatedUserCode
        )

        return userCreatePort.createUser(user)
    }

}