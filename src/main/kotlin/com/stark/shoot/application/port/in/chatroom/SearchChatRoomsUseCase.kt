package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.common.vo.UserId

interface SearchChatRoomsUseCase {
    fun searchChatRooms(userId: UserId, query: String?, type: String?, unreadOnly: Boolean?): List<ChatRoomResponse>
}