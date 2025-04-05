package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Component

@Component
class EventPublishFilter(
    private val eventPublisher: EventPublisher,
    private val loadChatRoomPort: LoadChatRoomPort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 채팅방 ID로 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 읽지 않은 메시지 수 이벤트 발행
        val unreadCounts = chatRoom.metadata.participantsMetadata.mapKeys { it.key.toString() }
            .mapValues { it.value.unreadCount }

        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = chatRoom.id.toString(),
                unreadCounts = unreadCounts,
                lastMessage = message.content.text
            )
        )

        return chain.proceed(message)
    }

}