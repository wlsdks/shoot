package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageReadSseController(
    private val processMessageUseCase: ProcessMessageUseCase
) {

    @Operation(
        summary = "메시지 읽음 처리 (SSE)",
        description = "해당 채팅방의 메시지를 읽음 처리하고 SSE로 채팅방 목록을 업데이트"
    )
    @PostMapping("/mark-read")
    fun markMessageRead(
        @RequestParam roomId: String,
        @RequestParam userId: String
    ): ResponseEntity<Unit> {
        processMessageUseCase.markAllMessagesAsRead(roomId, userId)
        return ResponseEntity.noContent().build()
    }

}