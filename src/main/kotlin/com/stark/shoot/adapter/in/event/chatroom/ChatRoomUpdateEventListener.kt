package com.stark.shoot.adapter.`in`.event.chatroom

import com.stark.shoot.application.service.chatroom.ChatRoomUpdateNotifyService
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatRoomUpdateEventListener(
    private val chatRoomUpdateNotifyService: ChatRoomUpdateNotifyService
) {

    @EventListener
    fun handleChatRoomUpdate(event: ChatRoomUpdateEvent) {
        chatRoomUpdateNotifyService.notify(event)
    }

}
