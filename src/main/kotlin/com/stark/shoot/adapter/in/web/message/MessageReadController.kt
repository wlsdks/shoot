package com.stark.shoot.adapter.`in`.web.message

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.application.port.`in`.message.RetrieveMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메시지", description = "메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class MessageReadController(
    private val retrieveMessageUseCase: RetrieveMessageUseCase
) {

    @Operation(
        summary = "메시지 조회 (커서 기반 페이지네이션)",
        description = "특정 채팅방(roomId)의 메시지를 `_id` 기준으로 페이지네이션하여 조회합니다."
    )
    @GetMapping("/get")
    fun getMessages(
        @RequestParam roomId: String,
        @RequestParam(required = false) lastId: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseDto<List<ChatMessage>> {
        return try {
            val messages = retrieveMessageUseCase.getMessages(roomId, lastId, limit)
            ResponseDto.success(messages)
        } catch (e: Exception) {
            throw ApiException(
                "메시지 조회에 실패했습니다: ${e.message}",
                ApiException.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                e
            )
        }
    }
    
}