package com.stark.shoot.infrastructure.config.security.service

import com.stark.shoot.application.port.out.user.RetrieveUserPort
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val retrieveUserPort: RetrieveUserPort
) : UserDetailsService {

    /**
     * 로그인 시 사용자 정보를 조회하는 메서드
     */
    override fun loadUserByUsername(username: String?): UserDetails {
        val findByUsername = username?.let { retrieveUserPort.findByUsername(it) }
            ?: throw IllegalArgumentException("User not found")
        return User(findByUsername.username) // 권한 or 패스워드 등 필요 시 추가
    }

}