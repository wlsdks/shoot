package com.stark.shoot.adapter.`in`.web.message.thread

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "스레드", description = "스레드 메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class ThreadMessageController(
    private val getThreadMessagesUseCase: GetThreadMessagesUseCase
) {

    @Operation(
        summary = "스레드 메시지 조회",
        description = "특정 스레드에 속한 메시지들을 조회합니다."
    )
    @GetMapping("/thread")
    fun getThreadMessages(
        @RequestParam threadId: String,
        @RequestParam(required = false) lastMessageId: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseDto<List<MessageResponseDto>> {
        val messages = getThreadMessagesUseCase.getThreadMessages(threadId, lastMessageId, limit)
        return ResponseDto.success(messages)
    }
}
