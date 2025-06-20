package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId

interface UpdateChatRoomFavoriteUseCase {
    fun updateFavoriteStatus(roomId: ChatRoomId, userId: UserId, isFavorite: Boolean): ChatRoomResponse
}