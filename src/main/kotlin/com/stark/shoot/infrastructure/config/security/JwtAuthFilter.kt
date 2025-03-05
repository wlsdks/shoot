package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.infrastructure.exception.web.JwtAuthenticationException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtAuthenticationService: JwtAuthenticationService
) : OncePerRequestFilter() {

    private val logger = KotlinLogging.logger {}

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1) Authorization 헤더에서 토큰 추출
        val token = extractToken(request)

        // 2) 토큰이 존재하면 인증 시도
        if (!token.isNullOrBlank()) {
            try {
                val authentication = jwtAuthenticationService.authenticateToken(token)
                // 3) 인증 성공 시 SecurityContext에 등록
                SecurityContextHolder.getContext().authentication = authentication
            } catch (ex: JwtAuthenticationException) {
                // JWT 검증 실패 시 SecurityContext에 인증 정보 세팅하지 않음
                logger.error(ex.message)
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private fun extractToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return bearer.removePrefix("Bearer ").trim()
    }

}