package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import com.stark.shoot.domain.chat.message.type.MessageType

data class MessageContentDocument(
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val metadata: MessageMetadataDocument? = null,
    val attachments: List<AttachmentDocument> = emptyList()
)