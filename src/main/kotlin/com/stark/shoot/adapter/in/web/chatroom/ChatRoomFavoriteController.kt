package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    fun updateFavorite(
        @RequestParam roomId: String,
        @RequestParam userId: String,
        @RequestParam isFavorite: Boolean
    ): ResponseDto<ChatRoomResponse> {
        return try {
            val updatedRoom = updateFavoriteUseCase.updateFavoriteStatus(roomId, userId, isFavorite)
            val message = if (isFavorite) "채팅방이 즐겨찾기에 추가되었습니다." else "채팅방이 즐겨찾기에서 제거되었습니다."
            ResponseDto.success(ChatRoomResponse.from(updatedRoom, userId.toObjectId()), message)
        } catch (e: Exception) {
            val errorCode = if (e.message?.contains("최대 핀") == true) {
                ApiException.TOO_MANY_PINNED_ROOMS
            } else {
                ApiException.INVALID_INPUT
            }
            throw ApiException(
                "즐겨찾기 설정에 실패했습니다: ${e.message}",
                errorCode,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}