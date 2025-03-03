package com.stark.shoot.infrastructure.config.security.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class CustomUserDetails(
    private val id: String,
    private val username: String
) : UserDetails {
    override fun getAuthorities() = listOf<GrantedAuthority>(SimpleGrantedAuthority("USER"))
    override fun getPassword() = "" // 비밀번호 필요 없음 (JWT 사용)
    override fun getUsername() = username // username 반환
    fun getId() = id // id 접근자 추가
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}