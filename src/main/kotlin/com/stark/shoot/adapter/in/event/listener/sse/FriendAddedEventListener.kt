package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.event.FriendAddedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class FriendAddedEventListener(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @EventListener
    fun handleFriendAddedEvent(event: FriendAddedEvent) {
        sseEmitterUseCase.sendFriendAddedEvent(event)
    }

}