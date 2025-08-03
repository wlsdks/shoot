package com.stark.shoot.adapter.`in`.rest.dto.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

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
        status = this.status ?: MessageStatus.SENT,
        readBy = this.readBy?.mapKeys { UserId.from(it.key.toLong()) }?.toMutableMap() ?: mutableMapOf(),
        metadata = metadata
    )
}

/**
 * 도메인 메시지의 메타데이터를 요청 DTO에 반영합니다.
 *
 * @param domainMessage 도메인 메시지
 */
fun ChatMessageRequest.updateFromDomain(domainMessage: ChatMessage) {
    val metadataDto = domainMessage.metadata.toRequestDto()
    this.tempId = metadataDto.tempId
    this.status = domainMessage.status
    this.metadata.needsUrlPreview = metadataDto.needsUrlPreview
    this.metadata.previewUrl = metadataDto.previewUrl
    this.metadata.urlPreview = metadataDto.urlPreview
}
