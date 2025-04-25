package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.message.impl.ChatRoomLoadFilter.Companion.CHAT_ROOM_CONTEXT_KEY
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReadStatusInitFilter(
    private val readStatusPort: ReadStatusPort
) : MessageProcessingFilter {

    @Transactional
    override fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 컨텍스트에서 채팅방 정보 가져오기
        val chatRoom = chain.getFromContext<ChatRoom>(CHAT_ROOM_CONTEXT_KEY)
            ?: return chain.proceed(message) // 채팅방 정보가 없으면 다음 필터로 진행

        // 메시지 처리 진행
        val processedMessage = chain.proceed(message)

        // 발신자의 마지막 읽은 메시지 ID 업데이트
        if (processedMessage.id != null) {
            readStatusPort.updateLastReadMessageId(
                roomId = message.roomId,
                userId = message.senderId,
                messageId = processedMessage.id.toString()
            )
        }

        return processedMessage
    }

}
