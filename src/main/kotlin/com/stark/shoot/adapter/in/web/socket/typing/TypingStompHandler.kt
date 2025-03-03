package com.stark.shoot.adapter.`in`.web.socket.typing

import com.stark.shoot.adapter.`in`.web.socket.dto.TypingIndicatorMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.util.concurrent.ConcurrentHashMap

@Controller
class TypingStompHandler(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}
    private val typingRateLimiter = ConcurrentHashMap<String, Long>()

    @Operation(
        summary = "타이핑 인디케이터 (작성중 표시)",
        description = """
            - 상대방이 글을 작성하면 "xxx님이 작성중입니다." 표시를 위함
            - 백엔드에서 타이핑 이벤트를 받을 때, 너무 자주 요청이 오면 무시하도록 제한을 겁니다. (예: 1초에 한 번만 처리)
        """
    )
    @MessageMapping("/typing")
    fun handleTypingIndicator(message: TypingIndicatorMessage) {
        val key = "${message.userId}:${message.roomId}"
        val now = System.currentTimeMillis()
        val lastSent = typingRateLimiter.getOrDefault(key, 0L)

        if (now - lastSent > 1000) { // 1초 제한
            logger.info { "Typing status: $message" }
            messagingTemplate.convertAndSend("/topic/typing/${message.roomId}", message)
            typingRateLimiter[key] = now
        }
    }

}