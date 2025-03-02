package com.stark.shoot.infrastructure.config.security.service

import com.stark.shoot.application.port.out.user.RetrieveUserPort
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val retrieveUserPort: RetrieveUserPort
) : UserDetailsService {

    override fun loadUserByUsername(
        username: String?
    ): UserDetails {
        // 사용자 조회
        val user = username?.let { retrieveUserPort.findByUsername(it) }
            ?: throw IllegalArgumentException("User not found")

        // UserDetails로 변환
        return CustomUserDetails(user.id.toString(), user.username)
    }

}