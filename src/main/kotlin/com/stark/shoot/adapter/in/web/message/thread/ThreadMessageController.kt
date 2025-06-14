package com.stark.shoot.adapter.`in`.web.message.thread

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.`in`.message.thread.SendThreadMessageUseCase
import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "스레드", description = "스레드 메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class ThreadMessageController(
    private val getThreadMessagesUseCase: GetThreadMessagesUseCase,
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val sendThreadMessageUseCase: SendThreadMessageUseCase,
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

    @Operation(
        summary = "스레드 상세 조회",
        description = "루트 메시지와 스레드 메시지를 함께 조회합니다."
    )
    @GetMapping("/thread/detail")
    fun getThreadDetail(
        @RequestParam threadId: String,
        @RequestParam(required = false) lastMessageId: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseDto<ThreadDetailDto> {
        val detail = getThreadDetailUseCase.getThreadDetail(threadId, lastMessageId, limit)
        return ResponseDto.success(detail)
    }

    @Operation(
        summary = "스레드 메시지 전송",
        description = "스레드에 새로운 메시지를 작성합니다."
    )
    @PostMapping("/thread")
    fun sendThreadMessage(
        @RequestBody request: ChatMessageRequest
    ): ResponseDto<Void> {
        sendThreadMessageUseCase.sendThreadMessage(request)
        return ResponseDto.success()
    }

}
