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
        val username = Username.from(username!!)

        // 사용자 조회
        val user = username.let { userQueryPort.findByUsername(it) }
            ?: throw IllegalArgumentException("User not found")

        // UserDetails로 변환
        return CustomUserDetails(user.id.toString(), user.username.value)
    }

}