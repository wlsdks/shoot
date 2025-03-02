package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.UserAuthUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.exception.UnauthorizedException
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class UserAuthService(
    private val retrieveUserPort: RetrieveUserPort
) : UserAuthUseCase {

    override fun retrieveAuthUserInformation(
        authentication: Authentication?
    ): UserResponse {
        if (authentication == null || !authentication.isAuthenticated) {
            throw UnauthorizedException("인증되지 않은 사용자입니다.")
        }

        val userId = ObjectId(authentication.name) // sub가 id디버깅
        val user = retrieveUserPort.findById(userId)
            ?: throw ResourceNotFoundException("해당 사용자를 찾을 수 없습니다: $userId")

        return user.toResponse()
    }

}