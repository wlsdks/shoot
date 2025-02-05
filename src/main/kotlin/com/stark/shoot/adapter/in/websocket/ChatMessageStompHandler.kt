package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.chat.SendMessageUseCase
import com.stark.shoot.infrastructure.common.exception.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatMessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @MessageMapping("/chat")
    fun handleChatMessage(
        message: ChatMessageRequest,
        principal: Principal
    ) {
        // principal.name -> userId
        logger.info { "Received message from ${principal.name}: ${message.content}" }

        // 1. Kafka로 메시지 발행 (비동기)
        sendMessageUseCase.handleMessage(message)
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    logger.error(throwable) { "Failed to process message" }
                    val errorResponse = ErrorResponse(
                        status = 500,
                        message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다.",
                        timestamp = System.currentTimeMillis()
                    )
                    messagingTemplate.convertAndSend(
                        "/topic/errors/${message.roomId}",
                        errorResponse
                    )
                }
            }

        // 2. 실시간으로 클라이언트에 전송
        messagingTemplate.convertAndSend(
            "/topic/messages/${message.roomId}",
            message
        )
    }

}