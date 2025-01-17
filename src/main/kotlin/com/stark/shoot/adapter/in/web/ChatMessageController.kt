package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.SendMessageRequest
import com.stark.shoot.application.port.`in`.SendMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/messages")
@RestController
class ChatMessageController(
    private val sendMessageUseCase: SendMessageUseCase
) {

    @PostMapping("/{roomId}/send")
    fun sendMessage(
        @PathVariable roomId: String,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<ChatMessage> {
        // roomId 경로 변수와 요청 바디의 roomId가 일치하는지 확인
        require(roomId == request.roomId) { "경로의 roomId와 요청 바디의 roomId가 일치하지 않습니다." }

        val message = sendMessageUseCase.sendMessage(
            roomId = roomId,
            senderId = request.senderId,
            messageContent = request.toDomain()
        )
        return ResponseEntity.ok(message)
    }

    @MessageMapping("/chat/send")
    @SendTo("/topic/messages")
    fun sendMessage(message: ChatMessageRequest): Map<String, String> {
        println("서버에서 메시지 수신: ${message.content}")
        val response = mapOf("status" to "success", "content" to "Received: ${message.content}")
        println("서버에서 응답 전송: $response")
        return response
    }

}