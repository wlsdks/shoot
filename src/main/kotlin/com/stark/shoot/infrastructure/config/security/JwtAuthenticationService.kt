package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import com.stark.shoot.infrastructure.config.security.service.CustomUserDetails
import com.stark.shoot.domain.exception.web.JwtAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class JwtAuthenticationService(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: UserDetailsService
) {

    /**
     * JWT 토큰을 검증하고 사용자 정보를 로드한 뒤,
     * Spring Security의 Authentication 객체를 만들어 반환합니다.
     */
    fun authenticateToken(token: String): Authentication {
        try {
            // 1) 토큰 유효성 확인
            if (!jwtProvider.isTokenValid(token)) {
                // isTokenValid()가 false면 잘못된 토큰
                throw JwtAuthenticationException("Invalid JWT token")
            }

            val userId = jwtProvider.extractId(token) // id 추출
            val username = jwtProvider.extractUsername(token) // username 추출
            val userDetails = userDetailsService.loadUserByUsername(username) as CustomUserDetails

            // authentication.name에 id 설정
            return UsernamePasswordAuthenticationToken(
                userId, // principal = id
                token,
                userDetails.authorities
            ).apply {
                details = userDetails // username 등 추가 정보
            }
        } catch (e: Exception) {
            throw if (e is JwtAuthenticationException) e
            else JwtAuthenticationException("JWT authentication failed")
        }
    }

}