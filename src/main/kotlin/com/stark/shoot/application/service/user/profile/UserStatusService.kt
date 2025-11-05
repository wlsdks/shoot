package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.profile.UserStatusUseCase
import com.stark.shoot.application.port.`in`.user.profile.command.UpdateUserStatusCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class UserStatusService(
    private val userCommandPort: UserCommandPort,
    private val userQueryPort: UserQueryPort
) : UserStatusUseCase {

    /**
     * 사용자 상태를 업데이트합니다.
     *
     * @param command 사용자 상태 업데이트 커맨드
     * @return 업데이트된 사용자 정보
     */
    override fun updateStatus(
        command: UpdateUserStatusCommand
    ): User {
        // 인증 정보에서 사용자 ID 추출
        val userId = UserId.from(command.authentication.name.toLong())

        // 사용자 정보 조회
        val user = userQueryPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 상태 변경이 필요한지 확인 (이미 같은 상태면 불필요한 업데이트 방지)
        if (user.status == command.status) {
            return user
        }

        // 상태 업데이트
        val updatedUser = user.copy(
            status = command.status,
            updatedAt = Instant.now(),
            lastSeenAt = if (command.status == UserStatus.ONLINE) Instant.now() else user.lastSeenAt
        )

        // 변경된 사용자 정보 저장
        return userCommandPort.updateUser(updatedUser)
    }

}
