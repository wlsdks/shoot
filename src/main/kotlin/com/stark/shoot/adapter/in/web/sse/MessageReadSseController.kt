package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
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
        @RequestParam userId: String,
        @RequestParam(required = false) requestId: String?
    ): ResponseDto<Unit> {
        return try {
            processMessageUseCase.markAllMessagesAsRead(roomId, userId, requestId)
            ResponseDto.success(Unit, "메시지가 읽음으로 처리되었습니다.")
        } catch (e: Exception) {
            throw ApiException(
                "메시지 읽음 처리에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}