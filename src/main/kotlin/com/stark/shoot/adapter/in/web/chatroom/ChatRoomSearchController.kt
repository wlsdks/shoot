package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방 검색", description = "채팅방 검색 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomSearchController(
    private val chatRoomSearchUseCase: ChatRoomSearchUseCase
) {

    @Operation(summary = "채팅방 검색", description = "채팅방을 검색합니다.")
    @GetMapping("/search")
    fun searchChatRooms(
        @RequestParam userId: Long,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) unreadOnly: Boolean?
    ): ResponseDto<List<ChatRoomResponse>> {
        val results = chatRoomSearchUseCase.searchChatRooms(userId, query, type, unreadOnly)
        return ResponseDto.success(results)
    }

}