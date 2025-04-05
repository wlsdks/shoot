package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

interface UpdateChatRoomFavoriteUseCase {
    fun updateFavoriteStatus(roomId: Long, userId: Long, isFavorite: Boolean): ChatRoom
}