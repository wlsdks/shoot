package com.stark.shoot.adapter.`in`.websocket.mark

import com.stark.shoot.adapter.`in`.websocket.dto.ChatReadRequest
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class MessageMarkStompHandler(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = KotlinLogging.logger {}

    @MessageMapping("/read")
    fun handleRead(request: ChatReadRequest) {
        val updatedMessage = processMessageUseCase.markMessageAsRead(request.messageId, request.userId)
        messagingTemplate.convertAndSend("/topic/messages/${updatedMessage.roomId}", updatedMessage)
        logger.info { "Message marked as read via WebSocket: messageId=${request.messageId}, userId=${request.userId}" }
    }

}