package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.message.impl.ChatRoomLoadFilter.Companion.CHAT_ROOM_CONTEXT_KEY
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ChatRoomUpdateFilter(
    private val saveChatRoomPort: SaveChatRoomPort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 컨텍스트에서 채팅방 정보 가져오기
        val chatRoom = chain.getFromContext<ChatRoom>(CHAT_ROOM_CONTEXT_KEY)
            ?: return chain.proceed(message) // 채팅방 정보가 없으면 다음 필터로 진행

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
