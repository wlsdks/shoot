package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.RetrieveChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "채팅방 목록", description = "사용자의 채팅방 목록 조회 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomListController(
    private val retrieveChatRoomUseCase: RetrieveChatRoomUseCase,
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @Operation(summary = "사용자의 채팅방 목록 조회", description = "특정 사용자의 채팅방 전체 목록을 조회합니다.")
    @GetMapping
    fun getChatRooms(
        @RequestParam userId: String
    ): ResponseEntity<List<ChatRoomResponse>> {
        val chatRooms = retrieveChatRoomUseCase.getChatRoomsForUser(userId.toObjectId())
        return ResponseEntity.ok(chatRooms)
    }

    @Operation(summary = "사용자의 채팅방 unreadCount 실시간 업데이트", description = "SSE로 사용자의 채팅방 unreadCount를 실시간으로 전송합니다.")
    @GetMapping(value = ["/updates/{userId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamUpdates(@PathVariable userId: String): SseEmitter {
        return sseEmitterUseCase.createEmitter(userId)
    }

}