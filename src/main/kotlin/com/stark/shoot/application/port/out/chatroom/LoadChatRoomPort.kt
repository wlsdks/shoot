package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

interface LoadChatRoomPort {
    fun findById(userId: Long): ChatRoom?
    fun findByParticipantId(participantId: Long): List<ChatRoom>
}