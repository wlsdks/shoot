package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class ReadStatusInitFilter(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val readStatusPort: ReadStatusPort
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

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