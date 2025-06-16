package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.event.FriendRemovedEvent
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseEmitterUseCase {
    fun createEmitter(userId: Long): SseEmitter
    fun sendUpdate(userId: Long, roomId: Long, unreadCount: Int, lastMessage: String?)
    fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent)
    fun sendFriendAddedEvent(event: FriendAddedEvent)
    fun sendFriendRemovedEvent(event: FriendRemovedEvent)
}