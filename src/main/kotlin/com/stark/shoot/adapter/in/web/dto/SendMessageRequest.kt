package com.stark.shoot.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent

data class SendMessageRequest @JsonCreator constructor(
    val senderId: String,
    val roomId: String,
    val content: String,
    val type: String
) {
    fun toDomain(): ChatMessage {
        return ChatMessage(
            roomId = roomId,
            senderId = senderId,
            content = MessageContent(
                text = content,
                type = MessageType.valueOf(type)
            ),
            status = MessageStatus.SENT
        )
    }
}
