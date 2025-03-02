package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserCreateService(
    private val userCreatePort: UserCreatePort,
    private val passwordEncoder: PasswordEncoder
) : UserCreateUseCase {

    // 예시: User 생성 시 자동으로 userCode 생성
    override fun createUser(username: String, nickname: String, password: String): User {
        val user = User(
            username = username,
            nickname = nickname,
            userCode = UUID.randomUUID().toString().substring(0, 8).uppercase(), // 간단한 8자리 코드 생성
            passwordHash = passwordEncoder.encode(password)
        )
        return userCreatePort.createUser(user)
    }

}