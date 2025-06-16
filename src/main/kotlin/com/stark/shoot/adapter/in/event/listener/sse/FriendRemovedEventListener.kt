package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.FriendRemovedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class FriendRemovedEventListener(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @EventListener
    fun handleFriendRemovedEvent(event: FriendRemovedEvent) {
        sseEmitterUseCase.sendFriendRemovedEvent(event)
    }
}
