package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.application.port.`in`.chatroom.command.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseEmitterUseCase {
    fun createEmitter(command: CreateEmitterCommand): SseEmitter
    fun sendUpdate(command: SendUpdateCommand)
    fun sendChatRoomCreatedEvent(command: SendChatRoomCreatedEventCommand)
    fun sendFriendAddedEvent(command: SendFriendAddedEventCommand)
    fun sendFriendRemovedEvent(command: SendFriendRemovedEventCommand)
}
