package com.stark.shoot.application.service.user.auth

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.auth.UserAuthUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.exception.web.UnauthorizedException
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class UserAuthService(
    private val findUserPort: FindUserPort
) : UserAuthUseCase {

    /**
     * 사용자 정보 조회 (인증된 사용자)
     *
     * @param authentication 인증 정보
     * @return 사용자 정보
     */
    override fun retrieveUserDetails(
        authentication: Authentication?
    ): UserResponse {
        if (authentication == null || !authentication.isAuthenticated) {
            throw UnauthorizedException("인증되지 않은 사용자입니다.")
        }

        val userId = ObjectId(authentication.name) // sub가 id디버깅
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("해당 사용자를 찾을 수 없습니다: $userId")

        return user.toResponse()
    }

}