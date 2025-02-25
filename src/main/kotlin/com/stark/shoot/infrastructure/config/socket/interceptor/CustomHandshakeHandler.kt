package com.stark.shoot.infrastructure.config.socket.interceptor

import com.stark.shoot.infrastructure.config.socket.StompPrincipal
import org.springframework.http.server.ServerHttpRequest
import org.springframework.security.core.Authentication
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

class CustomHandshakeHandler : DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal? {
        // attributes에 저장된 인증 정보를 사용
        val auth = attributes["authentication"] as? Authentication
        return if (auth != null) StompPrincipal(auth.name) else super.determineUser(request, wsHandler, attributes)
    }

}
