package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import java.time.Instant

data class ChatMessage(
    val id: String? = null,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContent,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val reactions: Map<String, Set<Long>> = emptyMap(),
    val mentions: Set<Long> = emptySet(),
    val createdAt: Instant? = Instant.now(),
    val updatedAt: Instant? = null,
    val isDeleted: Boolean = false,
    val readBy: MutableMap<Long, Boolean> = mutableMapOf(),
    var metadata: MutableMap<String, Any> = mutableMapOf(),

    // 메시지 고정기능
    val isPinned: Boolean = false,
    val pinnedBy: Long? = null,
    val pinnedAt: Instant? = null
)