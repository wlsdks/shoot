package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType

data class MessageContentDocument(
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,

    // 필요시 유지할 필드
    val metadata: MessageMetadataDocument? = null,
    val attachments: List<AttachmentDocument> = emptyList()
)