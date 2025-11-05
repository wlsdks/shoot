package com.stark.shoot.application.service.user.auth

import com.stark.shoot.adapter.`in`.rest.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.rest.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.auth.UserAuthUseCase
import com.stark.shoot.application.port.`in`.user.auth.command.RetrieveUserDetailsCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.exception.web.UnauthorizedException

@UseCase
class UserAuthService(
    private val userQueryPort: UserQueryPort
) : UserAuthUseCase {

    /**
     * 사용자 정보 조회 (인증된 사용자)
     *
     * @param command 사용자 정보 조회 커맨드
     * @return 사용자 정보
     */
    override fun retrieveUserDetails(
        command: RetrieveUserDetailsCommand
    ): UserResponse {
        val authentication = command.authentication

        if (authentication == null || !authentication.isAuthenticated) {
            throw UnauthorizedException("인증되지 않은 사용자입니다.")
        }

        val userId = UserId.from(authentication.name.toLong())

        val user = userQueryPort.findUserById(userId)
            ?: throw ResourceNotFoundException("해당 사용자를 찾을 수 없습니다: $userId")

        return user.toResponse()
    }

}
