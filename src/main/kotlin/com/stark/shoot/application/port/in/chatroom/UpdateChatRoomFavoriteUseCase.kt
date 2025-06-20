package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface UpdateChatRoomFavoriteUseCase {
    fun updateFavoriteStatus(roomId: ChatRoomId, userId: UserId, isFavorite: Boolean): ChatRoomResponse
}