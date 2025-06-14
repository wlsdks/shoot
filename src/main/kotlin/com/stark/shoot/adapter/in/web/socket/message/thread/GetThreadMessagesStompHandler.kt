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

    @Operation(
        summary = "스레드 메시지 조회 (WebSocket)",
        description = """
            특정 스레드의 하위 메시지(답글)를 페이징하여 조회합니다.
            스레드 목록에서 스크롤을 통해 추가 메시지를 가져올 때 `getThreadMessages`를 호출합니다.
        """
    )
    @MessageMapping("/thread/messages")
    fun handleGetThreadMessages(request: ThreadMessagesRequestDto) {
        val messages = getThreadMessagesUseCase
            .getThreadMessages(request.threadId, request.lastMessageId, request.limit)
        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/messages", messages)
    }

}
