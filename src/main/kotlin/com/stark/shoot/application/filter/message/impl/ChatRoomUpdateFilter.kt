package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
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
        val chatRoom = loadChatRoomPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 마지막 메시지 ID와 마지막 활동 시간만 업데이트
        val now = Instant.now()
        val needsUpdate = message.id != null &&
                (chatRoom.lastMessageId != message.id || chatRoom.lastActiveAt.isBefore(now.minusSeconds(60)))

        if (needsUpdate) {
            val updatedRoom = chatRoom.copy(
                lastMessageId = message.id,
                lastActiveAt = now
            )
            saveChatRoomPort.save(updatedRoom)
        }

        return chain.proceed(message)
    }

}