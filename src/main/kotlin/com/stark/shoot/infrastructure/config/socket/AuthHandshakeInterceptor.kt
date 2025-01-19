package com.stark.shoot.infrastructure.config.socket

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class AuthHandshakeInterceptor : HandshakeInterceptor {

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
        // 예: HTTP 헤더에서 JWT 토큰 추출
        val token = request.headers["Authorization"]?.firstOrNull()
            ?: return false // 토큰 없을 시 handshake 중단

        // 토큰 검증 후 유저 ID 파싱
//        val userId = validateAndParseToken(token)
//            ?: return false // 토큰 검증 실패 시 handshake 중단
//
//        attributes["userId"] = userId

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }

}