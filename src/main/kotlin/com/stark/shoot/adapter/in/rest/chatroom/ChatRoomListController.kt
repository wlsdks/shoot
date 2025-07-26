package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
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

    @Operation(summary = "채팅방 목록 조회", description = "사용자가 참여 중인 채팅방 목록을 반환합니다.")
    @GetMapping("/list")
    fun getChatRooms(
        @RequestParam userId: Long
    ): ResponseDto<List<ChatRoomResponse>> {
        val command = GetChatRoomsCommand.of(userId)
        val chatRooms = findChatRoomUseCase.getChatRoomsForUser(command)
        return ResponseDto.success(chatRooms)
    }

}
