package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType

data class MessageContent(
    val text: String,
    val type: MessageType,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,

    // 필요한 경우에만 남길 선택적 필드
    val metadata: MessageMetadata? = null,
    val attachments: List<Attachment> = emptyList()
)