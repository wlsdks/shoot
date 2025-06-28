package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.event.FriendRemovedEvent

/**
 * Command for sending a friend removed event
 */
data class SendFriendRemovedEventCommand(
    val event: FriendRemovedEvent
) {
    companion object {
        fun of(event: FriendRemovedEvent): SendFriendRemovedEventCommand {
            return SendFriendRemovedEventCommand(event)
        }
    }
}