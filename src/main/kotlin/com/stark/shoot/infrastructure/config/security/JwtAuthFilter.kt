package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    /**
     * HTTP 요청마다 실행되는 필터입니다.
     * Authorization 헤더의 Bearer 토큰을 추출해 검증 후,
     * 정상 토큰이면 SecurityContext에 인증 객체를 넣어줍니다.
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. Authorization 헤더에서 JWT 추출
        val token = extractToken(request)

        // 2. 유효한 토큰이면 userId(Principal) 꺼내서 인증 객체 등록
        if (!token.isNullOrBlank()) {
            try {
                val userId = jwtProvider.validateToken(token)
                val authentication = UsernamePasswordAuthenticationToken(
                    userId,            // principal: 사용자 식별자
                    null,    // credentials
                    emptyList()        // 권한(ROLE)
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (_: Exception) {
                // 검증 실패 시, SecurityContext에 인증 정보를 세팅하지 않음
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return bearer.removePrefix("Bearer ").trim()
    }

}