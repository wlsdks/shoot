package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ChatRoomReadUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방 읽음 처리", description = "채팅방 내 모든 메시지 읽음 처리 API")
@RestController
class ChatRoomReadController(
    private val chatRoomReadUseCase: ChatRoomReadUseCase
) {

    @Operation(
        summary = "모두 읽음 처리",
        description = "사용자가 채팅방에 들어가면, 해당 사용자의 unreadCount를 0으로 설정합니다.."
    )
    @PostMapping("/api/v1/chatrooms/{roomId}/readAll")
    fun markAllAsRead(
        @PathVariable roomId: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        chatRoomReadUseCase.markAllAsRead(roomId, userId)
        return ResponseEntity.ok().build()
    }

}
