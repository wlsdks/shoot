package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

data class MessageContentDocument(
    val text: String,
    val type: String, // "TEXT", "FILE", "URL", "EMOTICON"
    val metadata: MessageMetadataDocument? = null,
    val attachments: List<AttachmentDocument> = emptyList(),
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
)