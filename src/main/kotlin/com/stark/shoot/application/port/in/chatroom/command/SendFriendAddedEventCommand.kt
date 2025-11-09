package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.shared.event.FriendAddedEvent

/**
 * Command for sending a friend added event
 */
data class SendFriendAddedEventCommand(
    val event: FriendAddedEvent
) {
    companion object {
        fun of(event: FriendAddedEvent): SendFriendAddedEventCommand {
            return SendFriendAddedEventCommand(event)
        }
    }
}