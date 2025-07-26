package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.TitleRequest
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
import com.stark.shoot.application.port.`in`.chatroom.command.RemoveParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateTitleCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomController(
    private val findChatRoomUseCase: FindChatRoomUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val manageChatRoomUseCase: ManageChatRoomUseCase
) {

    @Operation(summary = "1:1 채팅방 생성", description = "특정 사용자와 친구의 1:1 채팅방을 생성합니다.")
    @PostMapping("/create/direct")
    fun createDirectChat(
        @RequestParam userId: Long,
        @RequestParam friendId: Long
    ): ResponseDto<ChatRoomResponse> {
        val command = CreateDirectChatCommand.of(userId, friendId)
        val room = createChatRoomUseCase.createDirectChat(command)

        return ResponseDto.success(room, "채팅방이 생성되었습니다.")
    }

    @Operation(summary = "사용자의 채팅방 목록 조회", description = "특정 사용자의 채팅방 전체 목록을 조회합니다.")
    @GetMapping
    fun getChatRooms(
        @RequestParam userId: Long
    ): ResponseDto<List<ChatRoomResponse>> {
        val command = GetChatRoomsCommand.of(userId)
        val chatRooms = findChatRoomUseCase.getChatRoomsForUser(command)
        return ResponseDto.success(chatRooms)
    }

    @Operation(summary = "채팅방 퇴장", description = "현재 사용자가 채팅방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/exit")
    fun exitChatRoom(
        @PathVariable roomId: Long,
        @RequestParam userId: Long
    ): ResponseDto<Boolean> {
        val command = RemoveParticipantCommand.of(roomId, userId)
        val result = manageChatRoomUseCase.removeParticipant(command)

        return ResponseDto.success(result, "채팅방에서 퇴장했습니다.")
    }

    @Operation(summary = "채팅방 제목 변경", description = "채팅방의 제목을 변경합니다.")
    @PutMapping("/{roomId}/title")
    fun updateTitle(
        @PathVariable roomId: Long,
        @RequestBody request: TitleRequest
    ): ResponseDto<Boolean> {
        val command = UpdateTitleCommand.of(roomId, request.title)
        val result = manageChatRoomUseCase.updateTitle(command)

        return ResponseDto.success(result, "채팅방 제목이 변경되었습니다.")
    }

}
