package com.stark.shoot.adapter.`in`.web.socket.message.thread

import com.stark.shoot.adapter.`in`.web.socket.dto.ThreadMessagesRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GetThreadMessagesStompHandler(
    private val getThreadMessagesUseCase: GetThreadMessagesUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @Operation(summary = "스레드 메시지 조회 (WebSocket)")
    @MessageMapping("/thread/messages")
    fun handleGetThreadMessages(request: ThreadMessagesRequestDto) {
        val messages = getThreadMessagesUseCase.getThreadMessages(request.threadId, request.lastMessageId, request.limit)
        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/messages", messages)
    }
}
