package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

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
        // 도메인 팩토리 메서드를 사용하여 사용자 생성
        val user = User.create(
            username = request.username,
            nickname = request.nickname,
            rawPassword = request.password,
            passwordEncoder = { rawPassword -> passwordEncoder.encode(rawPassword) },
            bio = request.bio,
            profileImageUrl = request.profileImage?.toString()
        )

        // 영속성 계층을 통해 사용자 저장
        return userCreatePort.createUser(user)
    }
}
