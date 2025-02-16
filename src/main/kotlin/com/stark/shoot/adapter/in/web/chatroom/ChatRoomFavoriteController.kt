package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방 즐겨찾기", description = "채팅방 즐겨찾기/고정 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomFavoriteController(
    private val updateChatRoomFavoriteUseCase: UpdateChatRoomFavoriteUseCase
) {

    @Operation(
        summary = "채팅방 즐겨찾기/고정",
        description = "사용자가 채팅방을 즐겨찾기(고정)로 등록하거나 해제합니다."
    )
    @PostMapping("/favorite")
    fun updateFavorite(
        @RequestParam roomId: String,
        @RequestParam userId: String,
        @RequestParam isFavorite: Boolean
    ): ChatRoomResponse {
        val updatedRoom = updateChatRoomFavoriteUseCase.updateFavoriteStatus(roomId, userId, isFavorite)
        return ChatRoomResponse.from(updatedRoom)  // 여기서 변환 후 반환
    }

}
