package com.stark.shoot.application.service.chat

import com.stark.shoot.application.port.`in`.chat.RetrieveMessageUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RetrieveMessageService(
    private val loadChatMessagePort: LoadChatMessagePort
) : RetrieveMessageUseCase {

    override fun getMessages(
        roomId: String,
        before: Instant?,
        limit: Int
    ): List<ChatMessage> {
        val message = if (before != null) {
            // 이전 메시지 조회
            loadChatMessagePort.findByRoomIdAndBeforeCreatedAt(roomId.toObjectId(), before)
        } else {
            // 최신 메시지 조회
            loadChatMessagePort.findByRoomId(roomId.toObjectId())
        }

        // 메시지가 limit보다 많을 경우 limit만큼만 반환
        return message.take(limit)
    }

}