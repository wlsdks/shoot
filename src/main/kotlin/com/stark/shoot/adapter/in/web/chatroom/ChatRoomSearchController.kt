package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
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
        @RequestParam userId: String,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) unreadOnly: Boolean?
    ): ResponseDto<List<ChatRoomResponse>> {
        return try {
            val results = chatRoomSearchUseCase.searchChatRooms(userId.toObjectId(), query, type, unreadOnly)
            ResponseDto.success(results)
        } catch (e: Exception) {
            throw ApiException(
                "채팅방 검색에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }
    
}