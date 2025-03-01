package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import java.time.Instant

data class ChatMessage(
    val id: String? = null,
    val roomId: String,
    val senderId: String,
    val content: MessageContent,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val reactions: Map<String, Set<String>> = emptyMap(),
    val mentions: Set<String> = emptySet(),
    val createdAt: Instant? = Instant.now(),
    val updatedAt: Instant? = null,
    val isDeleted: Boolean = false,
    val readBy: MutableMap<String, Boolean> = mutableMapOf(),
    var metadata: MutableMap<String, Any> = mutableMapOf() // 메타데이터 추가
)