package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.domain.user.vo.UserId
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
    private val chatRoomSearchUseCase: ChatRoomSearchUseCase,
    private val findChatRoomUseCase: FindChatRoomUseCase
) {

    @Operation(summary = "채팅방 검색", description = "채팅방을 검색합니다.")
    @GetMapping("/search")
    fun searchChatRooms(
        @RequestParam userId: Long,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) unreadOnly: Boolean?
    ): ResponseDto<List<ChatRoomResponse>> {
        val results = chatRoomSearchUseCase.searchChatRooms(UserId.from(userId), query, type, unreadOnly)
        return ResponseDto.success(results)
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
        val chatRoom = findChatRoomUseCase.findDirectChatBetweenUsers(
            UserId.from(myId),
            UserId.from(otherUserId)
        )

        return if (chatRoom != null) {
            ResponseDto.success(chatRoom, "채팅방을 찾았습니다.")
        } else {
            ResponseDto.fail("채팅방을 찾을 수 없습니다.", 404)
        }
    }

}