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

    // @MessageMapping("/chat")은 클라이언트가 메시지를 전송할 때 사용하는 서버 측 핸들러입니다.
    // 클라이언트가 채팅 메시지를 보낼 때, 이 핸들러가 호출되어 메시지를 받아 처리하고, 그 결과를 다시 /topic/messages/{roomId}로 전송합니다.
    @MessageMapping("/chat")
    fun handleChatMessage(
        message: ChatMessageRequest
    ) {
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

        // 2. 실시간으로 클라이언트에 전송 (클라이언트는 채팅방에 입장할 때, 해당 채팅방의 메시지를 받기 위해 이 채널을 구독합니다.)
        // 예를 들어, 채팅방 A의 모든 참가자는 /topic/messages/A를 구독하여 메시지가 도착하면 화면에 표시합니다. (즉, 실시간 채팅 내용을 보여주기 위한 채널입니다.)
        messagingTemplate.convertAndSend(
            "/topic/messages/${message.roomId}",
            message
        )
    }

    @MessageMapping("/typing")
    fun handleTypingIndicator(message: TypingIndicatorMessage) {
        logger.info { "타이핑 상태 전송: roomId=${message.roomId}, userId=${message.userId}, isTyping=${message.isTyping}" }
        // 타이핑 상태를 해당 채팅방의 구독자들에게 전파합니다.
        messagingTemplate.convertAndSend("/topic/typing/${message.roomId}", message)
    }

}