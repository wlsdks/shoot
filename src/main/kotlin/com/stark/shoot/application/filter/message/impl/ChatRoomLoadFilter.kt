package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.stereotype.Component

/**
 * 채팅방 로딩 필터
 *
 * 메시지 처리 과정에서 여러 필터가 채팅방 정보를 필요로 하므로,
 * 이 필터에서 한 번만 로딩하고 필터 체인 컨텍스트에 저장하여 재사용합니다.
 */
@Component
class ChatRoomLoadFilter(
    private val chatRoomQueryPort: ChatRoomQueryPort
) : MessageProcessingFilter {

    companion object {
        const val CHAT_ROOM_CONTEXT_KEY = "chatRoom"
    }

    override fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 채팅방 로딩
        val chatRoom = chatRoomQueryPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 필터 체인 컨텍스트에 채팅방 정보 저장
        chain.putInContext(CHAT_ROOM_CONTEXT_KEY, chatRoom)

        return chain.proceed(message)
    }
}
