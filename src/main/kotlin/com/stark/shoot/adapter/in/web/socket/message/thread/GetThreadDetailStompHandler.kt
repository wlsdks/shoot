package com.stark.shoot.adapter.`in`.web.socket.message.thread

import com.stark.shoot.adapter.`in`.web.socket.dto.ThreadDetailRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GetThreadDetailStompHandler(
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @Operation(summary = "스레드 상세 조회 (WebSocket)")
    @MessageMapping("/thread/detail")
    fun handleGetThreadDetail(request: ThreadDetailRequestDto) {
        val detail = getThreadDetailUseCase.getThreadDetail(request.threadId, request.lastMessageId, request.limit)
        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/detail", detail)
    }
}
