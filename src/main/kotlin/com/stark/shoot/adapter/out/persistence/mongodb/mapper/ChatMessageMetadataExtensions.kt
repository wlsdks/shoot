package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.web.dto.message.MessageMetadataResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import com.stark.shoot.domain.chat.message.ChatMessageMetadata

fun ChatMessageMetadata.toMessageMetadataDocument(): MessageMetadataDocument {
    return MessageMetadataDocument(
        tempId = tempId,
        needsUrlPreview = needsUrlPreview,
        previewUrl = previewUrl,
        urlPreview = urlPreview,
        readAt = readAt
    )
}

fun ChatMessageMetadata.toMessageMetadataResponseDto(): MessageMetadataResponseDto {
    return MessageMetadataResponseDto(
        tempId = tempId,
        needsUrlPreview = needsUrlPreview,
        previewUrl = previewUrl,
        urlPreview = urlPreview,
        readAt = readAt
    )
}
