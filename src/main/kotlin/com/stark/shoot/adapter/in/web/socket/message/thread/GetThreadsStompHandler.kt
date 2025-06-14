package com.stark.shoot.adapter.`in`.web.socket.message.thread

import com.stark.shoot.adapter.`in`.web.socket.dto.ThreadListRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GetThreadsStompHandler(
    private val getThreadsUseCase: GetThreadsUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @Operation(
        summary = "채팅방 스레드 목록 조회 (WebSocket)",
        description = "채팅방의 루트 메시지를 기준으로 생성된 스레드의 요약 정보(답글 수)를 페이징하여 제공합니다."
    )
    @MessageMapping("/threads")
    fun handleGetThreads(request: ThreadListRequestDto) {
        val threads = getThreadsUseCase.getThreads(request.roomId, request.lastThreadId, request.limit)
        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/threads", threads)
    }

}
