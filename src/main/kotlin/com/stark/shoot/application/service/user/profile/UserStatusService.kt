package com.stark.shoot.application.service.user.profile

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import com.stark.shoot.application.port.`in`.user.profile.UserStatusUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.common.util.toObjectId
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserStatusService(
    private val userUpdatePort: UserUpdatePort,
    private val findUserPort: FindUserPort,
    private val jwtProvider: JwtProvider
) : UserStatusUseCase {

    /**
     * 유저의 상태를 변경합니다.
     *
     * @param authentication 인증 정보
     * @param status 변경할 상태
     * @return 변경된 유저 상태 정보
     */
    override fun updateStatus(
        authentication: Authentication,
        status: UserStatus
    ): User {
        // JWT 토큰 추출
        val token = authentication.credentials.toString()

        // JWT 토큰에서 사용자 ID 추출해서 유저 정보 조회
        val userId = jwtProvider.extractId(token).toObjectId()
        val user = findUserPort.findUserById(userId)
            ?: throw IllegalArgumentException("User not found")

        // Jwt에서 username 추출
        val jwtUsername = jwtProvider.extractUsername(token)

        // 상태를 변경하는 유저가 자신이 맞는지 확인
        if (user.username != jwtUsername) {
            throw SecurityException("Unauthorized attempt to modify another user's status")
        }

        // 유저 상태 변경
        val updatedUser = user.copy(status = status, updatedAt = Instant.now())
        return userUpdatePort.updateUser(updatedUser)
    }

}