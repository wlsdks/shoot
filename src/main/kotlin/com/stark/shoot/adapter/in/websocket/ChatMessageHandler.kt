package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.application.port.`in`.chat.SendMessageUseCase
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatMessageHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * 클라이언트가 `/app/chat`로 메시지를 전송하면 호출됩니다.
     * 메시지를 저장한 후, `/topic/messages/{roomId}` 경로로 브로드캐스트합니다.
     *
     * @param message 클라이언트로부터 받은 메시지 요청 객체.
     */
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        println("Received message: ${message.content}")

//        val userId = principal.name // 현재 사용자 ID 가져오기

        // 1. kafka로 메시지 전송
        sendMessageUseCase.handleMessage(message)

        // 2. 실시간으로 클라이언트에 전송
        messagingTemplate.convertAndSend(
            "/topic/messages/${message.roomId}",
            message
        )
    }

}