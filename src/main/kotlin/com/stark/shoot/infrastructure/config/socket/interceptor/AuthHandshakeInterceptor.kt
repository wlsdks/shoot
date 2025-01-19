package com.stark.shoot.infrastructure.config.socket.interceptor

import com.stark.shoot.infrastructure.config.security.JwtAuthenticationService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class AuthHandshakeInterceptor(
    private val jwtAuthenticationService: JwtAuthenticationService
) : HandshakeInterceptor {

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
        val token = extractToken(request) ?: return false

        return try {
            // 1) `jwtAuthenticationService`를 통해 토큰 검증 + 사용자 로딩
            val authentication = jwtAuthenticationService.authenticateToken(token)

            // 2) 연결된 사용자 식별자(또는 인증 객체)를 attributes에 저장
            attributes["authentication"] = authentication
            attributes["userId"] = authentication.name

            true
        } catch (e: Exception) {
            false
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