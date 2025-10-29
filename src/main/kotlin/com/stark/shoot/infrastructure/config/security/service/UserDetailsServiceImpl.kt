package com.stark.shoot.infrastructure.config.security.service

import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.user.vo.Username
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userQueryPort: UserQueryPort
) : UserDetailsService {

    override fun loadUserByUsername(
        username: String?
    ): UserDetails {
        val usernameValue = username
            ?: throw IllegalArgumentException("Username cannot be null")

        val usernameVo = Username.from(usernameValue)

        // 사용자 조회
        val user = userQueryPort.findByUsername(usernameVo)
            ?: throw IllegalArgumentException("User not found")

        // UserDetails로 변환
        return CustomUserDetails(user.id.toString(), user.username.value)
    }

}