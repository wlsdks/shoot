package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "채팅방 목록", description = "사용자의 채팅방 목록 조회 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatUnreadCountController(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @Operation(
        summary = "사용자의 SSE 연결",
        description = """
            - 사용자의 채팅방 목록에 안읽은 메시지수와 마지막 메시지를 실시간으로 전송합니다.
            - 친구 추가시 상대방의 소셜 목록에 표시합니다.
            - 채팅방을 만들면 상대방의 채팅방 목록에 표시합니다.
        """
    )
    @GetMapping(
        value = ["/updates/{userId}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun streamUpdates(@PathVariable userId: Long): SseEmitter {
        return sseEmitterUseCase.createEmitter(userId)
    }

}