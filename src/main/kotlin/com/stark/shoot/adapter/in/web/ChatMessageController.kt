package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.SendMessageRequest
import com.stark.shoot.application.port.`in`.SendMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.web.bind.annotation.*

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class ChatMessageController(
    private val sendMessageUseCase: SendMessageUseCase
) {

    @Operation(
        summary = "메시지 전송 (REST)",
        description = "특정 채팅방(roomId)에 메시지를 전송합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "메시지 전송 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ChatMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "400", description = "잘못된 요청",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "500", description = "서버 오류",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @Parameters(
        Parameter(name = "roomId", description = "채팅방 ID", required = true, example = "12345"),
        Parameter(name = "senderId", description = "보낸 사람 ID", required = true, example = "user1")
    )
    @PostMapping("/{roomId}/send")
    fun sendMessage(
        @PathVariable roomId: String,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<ChatMessage> {
        require(roomId == request.roomId) { "경로의 roomId와 요청 바디의 roomId가 일치하지 않습니다." }

        val message = sendMessageUseCase.sendMessage(
            roomId = roomId,
            senderId = request.senderId,
            messageContent = request.toDomain()
        )
        return ResponseEntity.ok(message)
    }

    @Operation(
        summary = "메시지 전송 (WebSocket)",
        description = "WebSocket을 통해 메시지를 전송합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "메시지 전송 성공",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "500", description = "서버 오류",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @MessageMapping("/chat/send")
    @SendTo("/topic/messages")
    fun sendMessage(message: ChatMessageRequest): Map<String, String> {
        println("서버에서 메시지 수신: ${message.content}")
        val response = mapOf("status" to "success", "content" to "Received: ${message.content}")
        println("서버에서 응답 전송: $response")
        return response
    }

}
