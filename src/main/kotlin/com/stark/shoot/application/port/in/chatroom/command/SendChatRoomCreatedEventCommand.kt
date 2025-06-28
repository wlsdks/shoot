package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.event.ChatRoomCreatedEvent

/**
 * Command for sending a chat room created event
 */
data class SendChatRoomCreatedEventCommand(
    val event: ChatRoomCreatedEvent
) {
    companion object {
        fun of(event: ChatRoomCreatedEvent): SendChatRoomCreatedEventCommand {
            return SendChatRoomCreatedEventCommand(event)
        }
    }
}