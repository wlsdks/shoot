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
                logger.debug { "JWT authentication successful for request to ${request.requestURI}" }
            } catch (ex: JwtAuthenticationException) {
                // JWT 검증 실패 시 SecurityContext에 인증 정보 세팅하지 않음
                logger.error { "JWT authentication failed: ${ex.message} for request to ${request.requestURI}" }

                // 인증이 필요한 엔드포인트에 대한 요청인 경우에만 로그 레벨을 높임
                if (isProtectedEndpoint(request)) {
                    logger.warn { "Failed JWT authentication attempt to protected endpoint: ${request.requestURI}" }
                }
            } catch (ex: Exception) {
                // 예상치 못한 예외 처리
                logger.error { "JWT authentication failed: ${ex.message} for request to ${request.requestURI}" }
            }
        } else {
            // 토큰이 없는 경우 (정상적인 상황일 수 있음)
            logger.debug { "No JWT token found in request to ${request.requestURI}" }
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

        return protectedPatterns.any { pattern ->
            path.startsWith(pattern) || path == pattern
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private fun extractToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return bearer.removePrefix("Bearer ").trim()
    }

}
