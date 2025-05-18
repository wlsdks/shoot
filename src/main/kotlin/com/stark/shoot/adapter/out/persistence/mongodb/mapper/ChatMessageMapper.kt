package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.web.dto.message.AttachmentDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.UrlPreviewDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.AttachmentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import com.stark.shoot.domain.chat.message.*
import com.stark.shoot.domain.chat.reaction.MessageReactions
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ChatMessageMapper {

    fun toDocument(domain: ChatMessage): ChatMessageDocument {
        return ChatMessageDocument(
            roomId = domain.roomId,
            senderId = domain.senderId,
            content = toMessageContentDocument(domain.content),
            status = domain.status,
            replyToMessageId = domain.replyToMessageId?.let { ObjectId(it) },
            reactions = domain.reactions.mapValues { (_, userIds) ->
                userIds.map { it }.toSet()
            },
            mentions = domain.mentions.map { it }.toSet(),
            isDeleted = domain.isDeleted,
            readBy = domain.readBy.toMutableMap(),
            isPinned = domain.isPinned,
            pinnedBy = domain.pinnedBy,
            pinnedAt = domain.pinnedAt
        ).apply {
            id = domain.id?.let { ObjectId(it) }
            createdAt = domain.createdAt
        }
    }

    fun toDomain(document: ChatMessageDocument): ChatMessage {
        return ChatMessage(
            id = document.id?.toString(),
            roomId = document.roomId,
            senderId = document.senderId,
            content = toMessageContent(document.content),
            status = document.status,
            replyToMessageId = document.replyToMessageId?.toString(),
            messageReactions = MessageReactions(document.reactions.mapValues { (_, userIds) ->
                userIds.map { it }.toSet()
            }),
            mentions = document.mentions.map { it }.toSet(),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
            isDeleted = document.isDeleted,
            readBy = document.readBy.toMutableMap(),
            isPinned = document.isPinned,
            pinnedBy = document.pinnedBy,
            pinnedAt = document.pinnedAt
        )
    }

    // MessageContent <-> MessageContentDocument 변환
    private fun toMessageContentDocument(domain: MessageContent): MessageContentDocument {
        return MessageContentDocument(
            text = domain.text,
            type = domain.type,
            metadata = domain.metadata?.let { toMessageMetadataDocument(it) },
            attachments = domain.attachments.map { toAttachmentDocument(it) },
            isEdited = domain.isEdited,
            isDeleted = domain.isDeleted
        )
    }

    private fun toMessageContent(document: MessageContentDocument): MessageContent {
        return MessageContent(
            text = document.text,
            type = document.type,
            metadata = document.metadata?.let { toMessageMetadata(it) },
            attachments = document.attachments.map { toAttachment(it) },
            isEdited = document.isEdited,
            isDeleted = document.isDeleted
        )
    }

    // ChatMessageMetadata <-> MessageMetadataDocument 변환
    private fun toMessageMetadataDocument(domain: ChatMessageMetadata): MessageMetadataDocument {
        return MessageMetadataDocument(
            urlPreview = domain.urlPreview,
            readAt = domain.readAt
        )
    }

    private fun toMessageMetadata(document: MessageMetadataDocument): ChatMessageMetadata {
        return ChatMessageMetadata(
            urlPreview = document.urlPreview,
            readAt = document.readAt
        )
    }

    // Attachment <-> AttachmentDocument 변환
    private fun toAttachmentDocument(domain: Attachment): AttachmentDocument {
        return AttachmentDocument(
            id = domain.id,
            filename = domain.filename,
            contentType = domain.contentType,
            size = domain.size,
            url = domain.url,
            thumbnailUrl = domain.thumbnailUrl,
            metadata = domain.metadata
        )
    }

    private fun toAttachment(document: AttachmentDocument): Attachment {
        return Attachment(
            id = document.id,
            filename = document.filename,
            contentType = document.contentType,
            size = document.size,
            url = document.url,
            thumbnailUrl = document.thumbnailUrl,
            metadata = document.metadata
        )
    }

    // 도메인 Attachment를 AttachmentDto로 변환
    private fun toAttachmentDto(attachment: Attachment): AttachmentDto {
        return AttachmentDto(
            id = attachment.id,
            filename = attachment.filename,
            contentType = attachment.contentType,
            size = attachment.size,
            url = attachment.url,
            thumbnailUrl = attachment.thumbnailUrl
        )
    }

    // 도메인 모델(ChatMessage)을 MessageResponseDto로 변환
    fun toDto(message: ChatMessage): MessageResponseDto {
        return MessageResponseDto(
            id = message.id ?: "",
            roomId = message.roomId,
            senderId = message.senderId,
            content = MessageContentResponseDto(
                text = message.content.text,
                type = message.content.type,
                attachments = message.content.attachments.map { toAttachmentDto(it) },
                isEdited = message.content.isEdited,
                isDeleted = message.content.isDeleted,
                urlPreview = message.content.metadata?.urlPreview?.let { toUrlPreviewDto(it) }
            ),
            status = message.status,
            replyToMessageId = message.replyToMessageId,
            reactions = message.reactions,
            mentions = message.mentions,
            createdAt = message.createdAt,
            updatedAt = message.updatedAt,
            readBy = message.readBy,
            isPinned = message.isPinned,
            pinnedBy = message.pinnedBy,
            pinnedAt = message.pinnedAt
        )
    }

    // UrlPreview 도메인 객체를 DTO로 변환하는 매퍼
    private fun toUrlPreviewDto(preview: UrlPreview): UrlPreviewDto {
        return UrlPreviewDto(
            url = preview.url,
            title = preview.title,
            description = preview.description,
            imageUrl = preview.imageUrl,
            siteName = preview.siteName
        )
    }

    /**
     * 도메인 모델 리스트를 DTO 리스트로 변환
     */
    fun toDtoList(messages: List<ChatMessage>): List<MessageResponseDto> {
        return messages.map { toDto(it) }
    }

}
