package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class ReadStatusPersistenceAdapter(
    private val chatRoomUserRepository: ChatRoomUserRepository,
) : ReadStatusPort {

    override fun updateLastReadMessageId(
        roomId: ChatRoomId,
        userId: UserId,
        messageId: MessageId
    ) {
        chatRoomUserRepository.updateLastReadMessageId(roomId.value, userId.value, messageId.value)
    }

}