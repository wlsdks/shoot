package com.stark.shoot.infrastructure.config.socket.interceptor

import com.stark.shoot.infrastructure.config.security.JwtAuthenticationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class AuthHandshakeInterceptor(
    private val jwtAuthenticationService: JwtAuthenticationService
) : HandshakeInterceptor {

    private val logger = KotlinLogging.logger {}

    /**
     * WebSocket handshake 전에 호출되는 메서드 (인증 안된 사용자에게는 handshake 중단)
     * @return true: handshake 진행, false: handshake 중단
     */
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        // info 엔드포인트는 SockJS 내부에서 사용하는 경로로, 인증을 생략해야 500 오류가 발생하지 않습니다. (이것만으로 안먹힘 SockJs를 활성화해야 함)
        // 또한 /xhr, /xhr_streaming 등의 경로도 인증을 생략해야 합니다. (아니면 요청을 무한으로 보내게 됩니다.)
        val path = request.uri.path

        // SockJS fallback 요청 (예: /info, /xhr, /xhr_streaming 등)은 인증 없이 허용
        if (path.endsWith("/info") || path.contains("/xhr")) {
            return true
        }

        val token = extractToken(request)
        if (token == null) {
            logger.error { "No token provided in request: ${request.uri}" }
            return false
        }
        try {
            val authentication = jwtAuthenticationService.authenticateToken(token)
            attributes["authentication"] = authentication
            attributes["userId"] = authentication.name
            request.headers["user"] = listOf(authentication.name)
            logger.info { "Handshake successful for user: ${authentication.name}" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Handshake failed for token: $token" }
            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // 핸드쉐이크 후 처리 (필요한 경우)
    }

    private fun extractToken(request: ServerHttpRequest): String? {
        // 1. Authorization 헤더에 "Bearer ..." 형태 토큰이 있는지 확인
        val tokenFromHeader = request.headers["Authorization"]
            ?.firstOrNull()
            ?.removePrefix("Bearer ")
            ?.trim()

        if (!tokenFromHeader.isNullOrBlank()) {
            return tokenFromHeader
        }

        // 2. 요청 URL 파라미터에서 `token=`을 찾아볼 수도 있음
        val query = request.uri.query ?: return null
        return query.split("&")
            .find { it.startsWith("token=") }
            ?.substringAfter("token=")
    }

}