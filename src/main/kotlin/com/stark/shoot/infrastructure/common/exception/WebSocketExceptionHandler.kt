package com.stark.shoot.infrastructure.common.exception

import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component

@Component
class WebSocketExceptionHandler {

    @MessageExceptionHandler
    fun handleUnauthorizedException(ex: UnauthorizedException): WebSocketError {
        return WebSocketError(
            code = "UNAUTHORIZED",
            message = ex.message ?: "인증되지 않은 사용자입니다"
        )
    }

    @MessageExceptionHandler
    fun handleAccessDeniedException(ex: AccessDeniedException): WebSocketError {
        return WebSocketError(
            code = "FORBIDDEN",
            message = ex.message ?: "접근이 거부되었습니다"
        )
    }

}