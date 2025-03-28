package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ReadStatusInitFilter(
    private val loadChatRoomPort: LoadChatRoomPort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(message.roomId.toObjectId())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 읽음 상태 초기화 (발신자는 읽음, 나머지는 안읽음)
        val readBy = chatRoom.metadata.participantsMetadata.keys.associate {
            it.toString() to (it == ObjectId(message.senderId))
        }.toMutableMap()

        val updatedMessage = message.copy(readBy = readBy)
        return chain.proceed(updatedMessage)
    }

}