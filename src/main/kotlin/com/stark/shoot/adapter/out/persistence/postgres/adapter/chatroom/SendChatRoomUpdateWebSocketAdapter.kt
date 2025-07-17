package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.`in`.web.socket.dto.chatroom.ChatRoomUpdateDto
import com.stark.shoot.application.port.out.chatroom.SendChatRoomUpdatePort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SendChatRoomUpdateWebSocketAdapter(
    private val webSocketMessageBroker: WebSocketMessageBroker
) : SendChatRoomUpdatePort {

    override fun sendUpdate(
        userId: UserId,
        roomId: ChatRoomId,
        update: ChatRoomUpdateEvent.Update
    ) {
        val dto = ChatRoomUpdateDto(
            roomId = roomId.value,
            unreadCount = update.unreadCount,
            lastMessage = update.lastMessage
        )

        webSocketMessageBroker.sendMessage(
            "/user/${userId.value}/queue/room-update",
            dto
        )
    }

}