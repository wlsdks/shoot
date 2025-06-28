package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.domain.event.FriendRemovedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class FriendRemovedEventListener(
//    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @EventListener
    fun handleFriendRemovedEvent(event: FriendRemovedEvent) {
//        val command = SendFriendRemovedEventCommand.of(event)
//        sseEmitterUseCase.sendFriendRemovedEvent(command)
    }
}
