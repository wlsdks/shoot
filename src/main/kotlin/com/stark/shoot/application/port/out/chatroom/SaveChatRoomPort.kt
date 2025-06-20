package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom

interface SaveChatRoomPort {
    fun save(chatRoom: ChatRoom): ChatRoom
}