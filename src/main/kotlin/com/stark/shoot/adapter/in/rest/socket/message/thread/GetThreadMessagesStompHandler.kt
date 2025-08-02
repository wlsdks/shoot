package com.stark.shoot.adapter.`in`.rest.socket.message.thread

import com.stark.shoot.adapter.`in`.rest.socket.dto.ThreadMessagesRequestDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadMessagesCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GetThreadMessagesStompHandler(
    private val getThreadMessagesUseCase: GetThreadMessagesUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    /**
     * 스레드 메시지 조회 (WebSocket)
     * 특정 스레드의 하위 메시지(답글)를 페이징하여 조회합니다.
     * 스레드 목록에서 스크롤을 통해 추가 메시지를 가져올 때 `getThreadMessages`를 호출합니다.
     *
     * WebSocket Endpoint: /thread/messages
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/thread/messages")
    fun handleGetThreadMessages(request: ThreadMessagesRequestDto) {
        val command = GetThreadMessagesCommand.of(
            threadId = request.threadId,
            lastMessageId = request.lastMessageId,
            limit = request.limit
        )
        val messages = getThreadMessagesUseCase.getThreadMessages(command)

        messagingTemplate.convertAndSendToUser(request.userId.toString(), "/queue/thread/messages", messages)
    }

}
