package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.domain.exception.web.JwtAuthenticationException
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
        when {
            token != null -> {
                try {
                    val authentication = jwtAuthenticationService.authenticateToken(token)
                    SecurityContextHolder.getContext().authentication = authentication
                    logger.debug { "JWT authentication successful for request to ${request.requestURI}" }
                } catch (ex: JwtAuthenticationException) {
                    logger.error { "JWT authentication failed: ${ex.message} for request to ${request.requestURI}" }
                    if (isProtectedEndpoint(request)) {
                        logger.warn { "Failed JWT authentication attempt to protected endpoint: ${request.requestURI}" }
                    }
                } catch (ex: Exception) {
                    logger.error { "JWT authentication failed: ${ex.message} for request to ${request.requestURI}" }
                }
            }
            else -> logger.debug { "No JWT token found in request to ${request.requestURI}" }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 보호된 엔드포인트인지 확인
     * SecurityConfig에 정의된 인증이 필요한 엔드포인트 패턴과 일치하는지 확인
     */
    private fun isProtectedEndpoint(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        // 인증이 필요한 엔드포인트 패턴 목록
        // SecurityConfig의 authorizeHttpRequests 설정과 일치시켜야 함
        val protectedPatterns = listOf(
            "/api/v1/users/me",
            "/api/v1/messages/mark-read"
            // 필요에 따라 추가
        )

        return protectedPatterns.any { path.startsWith(it) }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

}
