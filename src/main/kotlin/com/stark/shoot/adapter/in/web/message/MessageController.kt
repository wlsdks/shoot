package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.application.port.`in`.message.MessageReadUseCase
import com.stark.shoot.application.port.`in`.message.RetrieveMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageController(
    private val retrieveMessageUseCase: RetrieveMessageUseCase,
    private val messageReadUseCase: MessageReadUseCase
) {

    @Operation(
        summary = "메시지 조회",
        description = "특정 채팅방(roomId)의 메시지를 조회합니다."
    )
    @GetMapping("/get")
    fun getMessages(
        @RequestParam roomId: String,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ChatMessage>> {
        val message = retrieveMessageUseCase.getMessages(roomId, before, limit)
        return ResponseEntity.ok(message)
    }

    @Operation(
        summary = "메시지 읽음 처리",
        description = "해당 채팅방의 메시지를 읽음 처리하여 unreadCount를 초기화합니다."
    )
    @PostMapping("/mark-read")
    fun markMessageRead(
        @RequestParam roomId: String,
        @RequestParam userId: String
    ) : ResponseEntity<Unit> {
        messageReadUseCase.markRead(roomId, userId)
        return ResponseEntity.noContent().build()
    }

}