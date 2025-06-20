package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.`in`.web.dto.chatroom.TitleRequest
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.chat.room.ChatRoomTitle
import com.stark.shoot.domain.common.vo.UserId
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
        val room = createChatRoomUseCase.createDirectChat(
            UserId.from(userId),
            UserId.from(friendId)
        )

        return ResponseDto.success(room, "채팅방이 생성되었습니다.")
    }

    @Operation(summary = "사용자의 채팅방 목록 조회", description = "특정 사용자의 채팅방 전체 목록을 조회합니다.")
    @GetMapping
    fun getChatRooms(
        @RequestParam userId: Long
    ): ResponseDto<List<ChatRoomResponse>> {
        val chatRooms = findChatRoomUseCase.getChatRoomsForUser(userId)
        return ResponseDto.success(chatRooms)
    }

    @Operation(summary = "채팅방 퇴장", description = "현재 사용자가 채팅방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/exit")
    fun exitChatRoom(
        @PathVariable roomId: Long,
        @RequestParam userId: Long
    ): ResponseDto<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(
            ChatRoomId.from(roomId),
            UserId.from(userId)
        )

        return ResponseDto.success(result, "채팅방에서 퇴장했습니다.")
    }

    @Operation(summary = "채팅방 제목 변경", description = "채팅방의 제목을 변경합니다.")
    @PutMapping("/{roomId}/title")
    fun updateTitle(
        @PathVariable roomId: Long,
        @RequestBody request: TitleRequest
    ): ResponseDto<Boolean> {
        val result = manageChatRoomUseCase.updateTitle(
            ChatRoomId.from(roomId),
            ChatRoomTitle.from(request.title)
        )

        return ResponseDto.success(result, "채팅방 제목이 변경되었습니다.")
    }

}
