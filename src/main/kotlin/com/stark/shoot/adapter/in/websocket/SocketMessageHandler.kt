package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class SocketMessageHandler(
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * 클라이언트가 `/app/chat`로 메시지를 전송하면 호출됩니다.
     * 메시지를 처리한 후 `/topic/messages/{roomId}` 경로로 브로드캐스트합니다.
     *
     * @param message 클라이언트로부터 받은 메시지 요청 객체.
     */
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        println("Received message: ${message.content}")

        // 동적으로 메시지를 특정 채팅방 경로로 브로드캐스트
        val destination = "/topic/messages/${message.roomId}"
        val response = ChatMessageResponse(status = "success", content = message.content)
        messagingTemplate.convertAndSend(destination, response)
    }

}
