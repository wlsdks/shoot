package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class ReadStatusPersistenceAdapter(
    private val chatRoomUserRepository: ChatRoomUserRepository,
) : ReadStatusPort {

    override fun updateLastReadMessageId(
        roomId: Long,
        userId: Long,
        messageId: String
    ) {
        chatRoomUserRepository.updateLastReadMessageId(roomId, userId, messageId)
    }

}