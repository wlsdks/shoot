package com.stark.shoot.adapter.`in`.socket.message.thread

import com.stark.shoot.adapter.`in`.socket.dto.ThreadDetailRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadDetailCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ThreadDetailStompHandler(
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    /**
     * 스레드 상세 조회 (WebSocket)
     * 스레드를 처음 열 때 사용하며 루트 메시지와 현재까지의 답글을 함께 반환합니다.
     *
     * WebSocket Endpoint: /thread/detail
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/thread/detail")
    fun handleGetThreadDetail(request: ThreadDetailRequestDto) {
        val command = GetThreadDetailCommand.of(request)
        val detail = getThreadDetailUseCase.getThreadDetail(command)

        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/detail", detail)
    }

}
