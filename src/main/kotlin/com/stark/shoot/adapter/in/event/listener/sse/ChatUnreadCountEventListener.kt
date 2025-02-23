package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatUnreadCountEventListener(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @EventListener
    fun handle(event: ChatUnreadCountUpdatedEvent) {
        event.unreadCounts.forEach { (userId, count) ->
            sseEmitterUseCase.sendUpdate(userId, event.roomId, count, event.lastMessage)
        }
    }

}