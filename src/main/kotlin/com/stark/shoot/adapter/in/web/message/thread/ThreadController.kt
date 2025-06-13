package com.stark.shoot.adapter.`in`.web.message.thread

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.thread.ThreadSummaryDto
import com.stark.shoot.application.port.`in`.message.thread.GetThreadsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "스레드", description = "스레드 메시지 관련 API")
@RequestMapping("/api/v1/messages")
@RestController
class ThreadController(
    private val getThreadsUseCase: GetThreadsUseCase,
) {

    @Operation(
        summary = "채팅방 스레드 목록 조회",
        description = "채팅방의 루트 메시지를 기반으로 스레드 목록을 조회합니다."
    )
    @GetMapping("/threads")
    fun getThreads(
        @RequestParam roomId: Long,
        @RequestParam(required = false) lastThreadId: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseDto<List<ThreadSummaryDto>> {
        val threads = getThreadsUseCase.getThreads(roomId, lastThreadId, limit)
        return ResponseDto.success(threads)
    }
}
