package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.event.ChatRoomUpdateEvent

interface ChatRoomUpdateNotifyUseCase {

    fun notify(event: ChatRoomUpdateEvent)

}