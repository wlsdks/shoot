package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class ChatMessageControllerTest {

    @MessageMapping("/chat/send")
    @SendTo("/topic/messages")
    fun sendMessage(message: ChatMessageRequest): Map<String, String> {
        println("서버에서 메시지 수신: ${message.content}")
        val response = mapOf("status" to "success", "content" to "Received: ${message.content}")
        println("서버에서 응답 전송: $response")
        return response
    }

}