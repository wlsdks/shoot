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
        // info 엔드포인트는 SockJS 내부에서 사용하는 경로로, 인증을 생략해야 500 오류가 발생하지 않습니다. (이것만으로 안먹힘 SockJs를 활성화해야 함)
        if (request.uri.path.endsWith("/info")) {
            return true
        }

        val token = extractToken(request) ?: return false

        return try {
            // JWT 토큰 검증 및 사용자 로딩
            val authentication = jwtAuthenticationService.authenticateToken(token)
            // 인증 객체와 사용자 ID를 attributes에 저장
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