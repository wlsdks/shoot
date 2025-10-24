package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.`in`.user.command.CreateUserCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.domain.event.UserCreatedEvent
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class UserCreateService(
    private val userCommandPort: UserCommandPort,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: EventPublishPort
) : UserCreateUseCase {

    private val logger = KotlinLogging.logger {}

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
        val savedUser = userCommandPort.createUser(user)

        // 사용자 생성 이벤트 발행 (트랜잭션 커밋 후 리스너들이 처리)
        publishUserCreatedEvent(savedUser)

        return savedUser
    }

    /**
     * 사용자 생성 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 웰컴 이메일, 초기 설정, 분석 등의 처리를 수행할 수 있습니다.
     */
    private fun publishUserCreatedEvent(user: User) {
        try {
            val event = UserCreatedEvent.create(
                userId = user.id ?: return,
                username = user.username.value,
                nickname = user.nickname.value,
                createdAt = user.createdAt ?: Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "UserCreatedEvent published for user ${user.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish UserCreatedEvent for user ${user.id?.value}" }
        }
    }

}
