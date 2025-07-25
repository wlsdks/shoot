package com.stark.shoot.adapter.`in`.rest.socket.message.thread

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.thread.SendThreadMessageUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.SendThreadMessageCommand
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class SendThreadMessageStompHandler(
    private val sendThreadMessageUseCase: SendThreadMessageUseCase,
) {

    @Operation(
        summary = "스레드 메시지 전송 (WebSocket)"
    )
    @MessageMapping("/thread")
    fun handleSendThreadMessage(request: ChatMessageRequest) {
        val command = SendThreadMessageCommand.of(request)
        sendThreadMessageUseCase.sendThreadMessage(command)
    }

}
