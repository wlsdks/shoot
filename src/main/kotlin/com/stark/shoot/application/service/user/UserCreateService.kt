package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
@UseCase
class UserCreateService(
    private val userCreatePort: UserCreatePort,
    private val passwordEncoder: PasswordEncoder
) : UserCreateUseCase {

    /**
     * 사용자 생성
     *
     * @param request 사용자 생성 요청
     * @return 생성된 사용자 정보
     */
    override fun createUser(
        request: CreateUserRequest
    ): User {
        val user = User(
            username = request.username,
            nickname = request.nickname,
            bio = request.bio,
            userCode = UUID.randomUUID().toString().substring(0, 8).uppercase(), // 간단한 8자리 코드 생성
            passwordHash = passwordEncoder.encode(request.password),
            profileImageUrl = request.profileImage.toString() // todo: 추후 이부분 수정 필요
        )

        return userCreatePort.createUser(user)
    }

}