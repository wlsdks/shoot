package com.stark.shoot.adapter.`in`.event.listener.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class ChatRoomCreatedEventListener(
    private val sseEmitterUseCase: SseEmitterUseCase
) {

    @EventListener
    fun handle(event: ChatRoomCreatedEvent) {
        sseEmitterUseCase.sendChatRoomCreatedEvent(event)
    }

}