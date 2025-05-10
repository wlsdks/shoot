package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "채팅방", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val manageChatRoomUseCase: ManageChatRoomUseCase,
    private val findChatRoomUseCase: FindChatRoomUseCase
) {

    @Operation(summary = "1:1 채팅방 생성", description = "특정 사용자와 친구의 1:1 채팅방을 생성합니다.")
    @PostMapping("/create/direct")
    fun createDirectChat(
        @RequestParam userId: Long,
        @RequestParam friendId: Long
    ): ResponseDto<ChatRoomResponse> {
        val room = createChatRoomUseCase.createDirectChat(userId, friendId)
        return ResponseDto.success(ChatRoomResponse.from(room, userId), "채팅방이 생성되었습니다.")
    }

    @Operation(summary = "채팅방 퇴장", description = "현재 사용자가 채팅방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/exit")
    fun exitChatRoom(
        @PathVariable roomId: Long,
        @RequestParam userId: Long
    ): ResponseDto<Boolean> {
        val result = manageChatRoomUseCase.removeParticipant(roomId, userId)
        return ResponseDto.success(result, "채팅방에서 퇴장했습니다.")
    }

    @Operation(
        summary = "두 사용자 간의 1:1 채팅방 찾기",
        description = "두 사용자 ID를 받아 해당 사용자들 간의 1:1 채팅방을 찾습니다."
    )
    @GetMapping("/direct")
    fun findDirectChatRoom(
        @RequestParam myId: Long,
        @RequestParam otherUserId: Long
    ): ResponseDto<ChatRoomResponse> {
        val chatRoom = findChatRoomUseCase.findDirectChatBetweenUsers(myId, otherUserId)
        return if (chatRoom != null) {
            ResponseDto.success(chatRoom, "채팅방을 찾았습니다.")
        } else {
            ResponseDto.fail("채팅방을 찾을 수 없습니다.", 404)
        }
    }

}
