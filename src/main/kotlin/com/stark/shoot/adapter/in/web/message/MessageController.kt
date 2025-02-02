package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.application.port.`in`.chat.RetrieveMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageController(
    private val retrieveMessageUseCase: RetrieveMessageUseCase
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

}