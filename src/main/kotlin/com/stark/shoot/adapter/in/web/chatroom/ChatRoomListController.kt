package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방 목록", description = "사용자의 채팅방 목록 조회 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomListController(
    private val findChatRoomUseCase: FindChatRoomUseCase
) {

    @Operation(
        summary = "사용자의 채팅방 목록 조회",
        description = "특정 사용자의 채팅방 전체 목록을 조회합니다."
    )
    @GetMapping
    fun getChatRooms(
        @RequestParam userId: Long
    ): ResponseDto<List<ChatRoomResponse>> {
        val chatRooms = findChatRoomUseCase.getChatRoomsForUser(userId)
        return ResponseDto.success(chatRooms)
    }

}