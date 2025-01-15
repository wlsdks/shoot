package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType

data class MessageContent(
    val text: String,
    val type: MessageType,
    val metadata: MessageMetadata? = null,
    val attachments: List<Attachment> = emptyList(),
    var isEdited: Boolean = false,
    var isDeleted: Boolean = false
)