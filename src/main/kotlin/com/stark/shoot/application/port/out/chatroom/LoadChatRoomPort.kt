package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface LoadChatRoomPort {
    fun findById(roomId: ChatRoomId): ChatRoom?
    fun findByParticipantId(participantId: UserId): List<ChatRoom>
}