package com.stark.shoot.adapter.`in`.web.message.thread

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadDetailDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadDetailUseCase
import com.stark.shoot.application.port.`in`.message.thread.GetThreadMessagesUseCase
import com.stark.shoot.application.port.`in`.message.thread.SendThreadMessageUseCase
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
        description = """
            특정 스레드의 하위 메시지(답글)를 페이징하여 조회합니다.
            스레드 목록에서 스크롤을 통해 추가 메시지를 가져올 때 `getThreadMessages`를 호출합니다.
        """
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
        description = "스레드를 처음 열 때 사용하며 루트 메시지와 현재까지의 답글을 함께 반환합니다."
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
        description = "지정된 threadId의 스레드에 새 메시지를 작성합니다."
    )
    @PostMapping("/thread")
    fun sendThreadMessage(
        @RequestBody request: ChatMessageRequest
    ): ResponseDto<Void> {
        sendThreadMessageUseCase.sendThreadMessage(request)
        return ResponseDto.success()
    }

}
