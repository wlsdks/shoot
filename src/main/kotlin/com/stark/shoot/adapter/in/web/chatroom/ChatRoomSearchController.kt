package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomSearchController(
    private val chatRoomSearchUseCase: ChatRoomSearchUseCase
) {

    @Operation(summary = "채팅방 검색", description = "채팅방을 검색합니다.")
    @GetMapping("/search")
    fun searchChatRooms(
        @RequestParam userId: String,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) unreadOnly: Boolean?
    ): ResponseEntity<List<ChatRoomResponse>> {
        val results = chatRoomSearchUseCase.searchChatRooms(userId.toObjectId(), query, type, unreadOnly)
        return ResponseEntity.ok(results)
    }

}