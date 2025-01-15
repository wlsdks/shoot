package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType

data class MessageContentDocument(
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val metadata: MessageMetadataDocument? = null,
    val attachments: List<AttachmentDocument> = emptyList(),
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
)