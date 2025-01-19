package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.infrastructure.common.exception.JwtAuthenticationException
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
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

            // 2) 토큰에서 사용자 이름 추출
            val username = jwtProvider.extractUsername(token)

            // 3) DB/저장소에서 사용자 정보 로드 (UserDetailsService)
            val userDetails = userDetailsService.loadUserByUsername(username)

            // 4) 인증 객체 생성 (Principal=userDetails, Credentials=token, Authorities=userDetails.authorities)
            return UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                userDetails.authorities
            )
        } catch (e: Exception) {
            // 구체적인 예외 처리
            throw if (e is JwtAuthenticationException) e
            else JwtAuthenticationException("JWT authentication failed")
        }
    }

}