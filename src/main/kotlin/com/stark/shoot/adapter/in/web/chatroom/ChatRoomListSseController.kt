package com.stark.shoot.adapter.`in`.web.chatroom

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
class ChatRoomListSseController(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @Operation(
        summary = "사용자의 채팅방 unreadCount 실시간 업데이트",
        description = "SSE로 사용자의 채팅방 unreadCount를 실시간으로 전송합니다."
    )
    @GetMapping(value = ["/updates/{userId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamUpdates(@PathVariable userId: String): SseEmitter {
        return sseEmitterUseCase.createEmitter(userId)
    }

}