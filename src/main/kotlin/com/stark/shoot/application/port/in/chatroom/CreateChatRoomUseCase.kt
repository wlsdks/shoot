package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.user.vo.UserId

interface CreateChatRoomUseCase {
    fun createDirectChat(userId: UserId, friendId: UserId): ChatRoomResponse
}