package com.stark.shoot.application.service.message.filter

import com.stark.shoot.application.port.`in`.message.process.MessageProcessingChain
import com.stark.shoot.application.port.`in`.message.process.MessageProcessingFilter
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ChatRoomUpdateFilter(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        val property = message.metadata["property"] as? MessageProperty ?: return chain.proceed(message)

        val chatRoom = loadChatRoomPort.findById(message.roomId.toObjectId())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 채팅방 업데이트
        val updatedRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(participantsMetadata = property.updatedParticipants),
            lastMessageId = message.id,
            lastMessageText = message.content.text,
            lastActiveAt = Instant.now()
        )

        saveChatRoomPort.save(updatedRoom)

        // 메시지에서 임시 프로퍼티 제거
        message.metadata.remove("property")

        return chain.proceed(message)
    }

}