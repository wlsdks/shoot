package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.message.impl.ChatRoomLoadFilter.Companion.CHAT_ROOM_CONTEXT_KEY
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.stereotype.Component

@Component
class EventPublishFilter(
    private val eventPublisher: EventPublisher
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 컨텍스트에서 채팅방 정보 가져오기
        val chatRoom = chain.getFromContext<ChatRoom>(CHAT_ROOM_CONTEXT_KEY)
            ?: return chain.proceed(message) // 채팅방 정보가 없으면 다음 필터로 진행

        // 읽지 않은 메시지 수 이벤트 발행
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = chatRoom.id!!,
                unreadCounts = mapOf(),
                lastMessage = message.content.text
            )
        )

        return chain.proceed(message)
    }

}
