package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.event.FriendRemovedEvent
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseEmitterUseCase {
    fun createEmitter(userId: UserId): SseEmitter
    fun sendUpdate(userId: UserId, roomId: ChatRoomId, unreadCount: Int, lastMessage: String?)
    fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent)
    fun sendFriendAddedEvent(event: FriendAddedEvent)
    fun sendFriendRemovedEvent(event: FriendRemovedEvent)
}