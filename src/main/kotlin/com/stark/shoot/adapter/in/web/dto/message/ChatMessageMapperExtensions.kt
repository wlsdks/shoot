package com.stark.shoot.adapter.`in`.web.dto.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId

fun ChatMessageMetadataRequest.toDomain(): ChatMessageMetadata {
    return ChatMessageMetadata(
        tempId = this.tempId,
        needsUrlPreview = this.needsUrlPreview,
        previewUrl = this.previewUrl,
        urlPreview = this.urlPreview,
        readAt = this.readAt
    )
}

fun ChatMessageMetadata.toResponseDto(): MessageMetadataResponseDto {
    return MessageMetadataResponseDto(
        tempId = tempId,
        needsUrlPreview = needsUrlPreview,
        previewUrl = previewUrl,
        urlPreview = urlPreview
    )
}

fun ChatMessageMetadata.toRequestDto(): ChatMessageMetadataRequest {
    return ChatMessageMetadataRequest(
        tempId = tempId,
        needsUrlPreview = needsUrlPreview,
        previewUrl = previewUrl,
        urlPreview = urlPreview,
        readAt = readAt
    )
}

fun ChatMessageRequest.toDomain(): ChatMessage {
    val content = MessageContent(
        text = this.content.text,
        type = this.content.type,
        isEdited = this.content.isEdited,
        isDeleted = this.content.isDeleted,
        attachments = emptyList()
    )

    val metadata = this.metadata.toDomain()

    return ChatMessage(
        id = this.id?.let { MessageId.from(it) },
        roomId = ChatRoomId.from(this.roomId),
        senderId = UserId.from(this.senderId),
        content = content,
        threadId = this.threadId?.let { MessageId.from(it) },
        status = this.status ?: MessageStatus.SAVED,
        readBy = this.readBy?.mapKeys { UserId.from(it.key.toLong()) }?.toMutableMap() ?: mutableMapOf(),
        metadata = metadata
    )
}
