package com.stark.shoot.domain.chat.message.vo

import com.stark.shoot.domain.chat.message.type.MessageType

data class MessageContent(
    val text: String,
    val type: MessageType,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,

    // 필요한 경우에만 남길 선택적 필드
    val metadata: ChatMessageMetadata? = null,
    val attachments: List<Attachment> = emptyList()
) {
    companion object {
    }

    data class Attachment(
        val id: String,
        val filename: String,
        val contentType: String,
        val size: Long,
        val url: String,
        val thumbnailUrl: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    )
}