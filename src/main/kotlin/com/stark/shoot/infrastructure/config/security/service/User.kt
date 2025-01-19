package com.stark.shoot.infrastructure.config.security.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class User(
    private val username: String,
    private val password: String? = null,
    private val authorities: Set<GrantedAuthority>? = setOf(SimpleGrantedAuthority("USER")),
    private val enabled: Boolean = true
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return authorities.orEmpty().toMutableSet()
    }

    override fun getPassword(): String {
        return password.orEmpty()
    }

    override fun getUsername(): String {
        return username
    }

}