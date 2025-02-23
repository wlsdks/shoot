package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.infrastructure.common.exception.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatMessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    // 클라이언트가 /app/chat로 메시지를 보내면 Kafka로 발행, 실패 시 /topic/errors/{roomId}로 에러 전송.
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        // Kafka로만 발행, WebSocket 푸시는 Consumer에서 수행
        sendMessageUseCase.handleMessage(message)
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    logger.error(throwable) { "Failed to publish message to Kafka: ${message.content.text}" }
                    val errorResponse = ErrorResponse(
                        status = 500,
                        message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다.",
                        timestamp = System.currentTimeMillis()
                    )
                    messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)
                } else {
                    logger.debug { "Message successfully published to Kafka: ${message.content.text}" }
                }
            }
    }

    @MessageMapping("/typing")
    fun handleTypingIndicator(message: TypingIndicatorMessage) {
        logger.info { "Typing status broadcasted: roomId=${message.roomId}, userId=${message.userId}, username=${message.username}, isTyping=${message.isTyping}" }
        // 타이핑 인디케이터 메시지를 해당 채팅방의 모든 사용자에게 전파
        messagingTemplate.convertAndSend("/topic/typing/${message.roomId}", message)
    }

}