package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.UserAuthUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.exception.UnauthorizedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class UserAuthService(
    private val retrieveUserPort: RetrieveUserPort
) : UserAuthUseCase {

    override fun retrieveAuthUserInformation(
        authentication: Authentication?
    ): UserResponse {
        // 1) 인증 객체가 없거나 인증 안 된 상태면 예외
        if (authentication == null || !authentication.isAuthenticated) {
            throw UnauthorizedException("인증되지 않은 사용자입니다.")
        }

        // 2) authentication.name -> 일반적으로 username(혹은 token subject)
        val username = authentication.name

        // 3) DB/저장소에서 사용자 정보 조회
        //    (UserDocument -> Domain User -> ...)
        val user: User = retrieveUserPort.findByUsername(username)
            ?: throw ResourceNotFoundException("해당 사용자를 찾을 수 없습니다: $username")

        // 4) (기존) UserResponse 로 변환하여 반환
        //    만약 UserResponse(id, username, nickname, ...) 구조가 있다면 맞춰서 매핑
        val response = UserResponse(
            id = user.id.toString(),       // user.id가 ObjectId? -> toString() 또는 toHexString()
            username = user.username,
            nickname = user.nickname
            // 필요시 profileImageUrl, lastSeenAt 등 추가
        )

        return response
    }

}