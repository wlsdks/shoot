package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.event.FriendRemovedEvent
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseEmitterUseCase {
    fun createEmitter(userId: UserId): SseEmitter
    fun sendUpdate(userId: UserId, roomId: ChatRoomId, unreadCount: Int, lastMessage: String?)
    fun sendChatRoomCreatedEvent(event: ChatRoomCreatedEvent)
    fun sendFriendAddedEvent(event: FriendAddedEvent)
    fun sendFriendRemovedEvent(event: FriendRemovedEvent)
}