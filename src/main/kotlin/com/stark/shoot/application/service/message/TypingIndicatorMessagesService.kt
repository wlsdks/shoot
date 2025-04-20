package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.`in`.web.socket.dto.TypingIndicatorMessage
import com.stark.shoot.application.port.`in`.message.TypingIndicatorMessageUseCase
import com.stark.shoot.infrastructure.annotation.UseCase
import java.util.concurrent.ConcurrentHashMap

@UseCase
class TypingIndicatorMessagesService(
    private val webSocketMessageBroker: WebSocketMessageBroker
) : TypingIndicatorMessageUseCase {

    private val typingRateLimiter = ConcurrentHashMap<String, Long>()

    /**
     * 타이핑 인디케이터 메시지를 전송합니다.
     *
     * @param message 타이핑 인디케이터 메시지
     */
    override fun sendMessage(message: TypingIndicatorMessage) {
        val key = "${message.userId}:${message.roomId}"
        val now = System.currentTimeMillis()
        val lastSent = typingRateLimiter.getOrDefault(key, 0L)

        if (now - lastSent > 1000) { // 1초 제한
            webSocketMessageBroker.sendMessage(
                "/topic/typing/${message.roomId}",
                message
            )

            typingRateLimiter[key] = now
        }
    }

}
