package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.`in`.user.command.CreateUserCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserCreateService(
    private val userCommandPort: UserCommandPort,
    private val passwordEncoder: PasswordEncoder
) : UserCreateUseCase {

    /**
     * 사용자 생성
     *
     * @param command 사용자 생성 커맨드
     * @return 생성된 사용자 정보
     */
    override fun createUser(
        command: CreateUserCommand
    ): User {
        // 도메인 팩토리 메서드를 사용하여 사용자 생성
        val user = User.create(
            username = command.username.value,
            nickname = command.nickname.value,
            rawPassword = command.password,
            passwordEncoder = { rawPassword -> passwordEncoder.encode(rawPassword) },
            bio = command.bio?.value,
            profileImageUrl = command.profileImage?.toString()
        )

        // 영속성 계층을 통해 사용자 저장
        return userCommandPort.createUser(user)
    }

}
