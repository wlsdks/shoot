package com.stark.shoot.adapter.`in`.socket.message

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.SendMessageCommand
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class MessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase
) {

    /**
     * 클라이언트로부터 메시지를 수신하여 처리합니다.
     * 1. 메시지에 임시 ID와 "sending" 상태 추가
     * 2. Redis를 통해 메시지 즉시 브로드캐스트 (실시간성)
     * 3. Kafka를 통해 메시지 영속화 (안정성)
     * 4. 메시지 상태 업데이트를 클라이언트에 전송
     *
     * WebSocket Endpoint: /chat
     * Protocol: STOMP over WebSocket
     */
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        val command = SendMessageCommand.of(message)
        sendMessageUseCase.sendMessage(command)
    }

}
