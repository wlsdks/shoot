package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface UpdateChatRoomFavoriteUseCase {
    fun updateFavoriteStatus(roomId: Long, userId: Long, isFavorite: Boolean): ChatRoomResponse
}