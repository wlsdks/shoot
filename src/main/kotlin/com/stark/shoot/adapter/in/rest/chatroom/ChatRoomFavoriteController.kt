package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomFavoriteRequest
import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateFavoriteStatusCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방 즐겨찾기", description = "채팅방 즐겨찾기/고정 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatRoomFavoriteController(
    private val updateFavoriteUseCase: UpdateChatRoomFavoriteUseCase
) {

    @Operation(
        summary = "채팅방 즐겨찾기/고정",
        description = "사용자가 채팅방을 즐겨찾기(고정)로 등록하거나 해제합니다."
    )
    @PostMapping("/favorite")
    fun updateFavorite(@RequestBody request: ChatRoomFavoriteRequest): ResponseDto<ChatRoomResponse> {
        val command = UpdateFavoriteStatusCommand.of(request)
        val updatedRoom = updateFavoriteUseCase.updateFavoriteStatus(command)
        val message = if (request.isFavorite) "채팅방이 즐겨찾기에 추가되었습니다." else "채팅방이 즐겨찾기에서 제거되었습니다."
        return ResponseDto.success(updatedRoom, message)
    }

}
