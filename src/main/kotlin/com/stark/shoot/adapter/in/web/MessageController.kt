package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.application.port.`in`.RetrieveMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RequestMapping("/api/v1/messages")
@RestController
class MessageController(
    private val retrieveMessageUseCase: RetrieveMessageUseCase
) {

    @GetMapping
    fun getMessages(
        @RequestParam roomId: String,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ChatMessage>> {
        val message = retrieveMessageUseCase.getMessages(roomId, before, limit)
        return ResponseEntity.ok(message)
    }

}