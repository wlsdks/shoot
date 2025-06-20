package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import java.time.Instant

data class MessageMetadataDocument(
    val tempId: String? = null,
    val needsUrlPreview: Boolean = false,
    val previewUrl: String? = null,
    val urlPreview: ChatMessageMetadata.UrlPreview? = null,
    var readAt: Instant? = null
) {
    fun toMessageMetadata(): ChatMessageMetadata {
        return ChatMessageMetadata(
            tempId = tempId,
            needsUrlPreview = needsUrlPreview,
            previewUrl = previewUrl,
            urlPreview = urlPreview
        )
    }
}