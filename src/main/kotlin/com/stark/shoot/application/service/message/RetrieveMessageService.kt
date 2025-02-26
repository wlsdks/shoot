package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.RetrieveMessageUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service

@Service
class RetrieveMessageService(
    private val loadChatMessagePort: LoadChatMessagePort
) : RetrieveMessageUseCase {

    override fun getMessages(
        roomId: String,
        lastId: String?,
        limit: Int
    ): List<ChatMessage> {
        return if (lastId != null) {
            loadChatMessagePort.findByRoomIdAndBeforeId(roomId.toObjectId(), lastId.toObjectId(), limit)
        } else {
            loadChatMessagePort.findByRoomId(roomId.toObjectId(), limit)
        }
    }

}
