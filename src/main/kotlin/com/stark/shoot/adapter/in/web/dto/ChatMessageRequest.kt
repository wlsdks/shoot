package com.stark.shoot.adapter.`in`.web.dto

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent

data class ChatMessageRequest(
    val roomId: String,       // 채팅방 ID
    val senderId: String,     // 보낸 사람 ID
    val content: String       // 메시지 내용
) {
    // ChatMessageRequest -> ChatMessage 변환
    fun toDomain(): ChatMessage {
        return ChatMessage(
            roomId = roomId,
            senderId = senderId,
            content = MessageContent(
                text = content,        // 메시지 내용 설정
                type = MessageType.TEXT // 예: 기본적으로 TEXT로 설정
            ),
            status = MessageStatus.SENT // 메시지 상태를 기본으로 SENT로 설정
        )
    }

}
