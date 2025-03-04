package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.AttachmentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.UrlPreviewDocument
import com.stark.shoot.domain.chat.message.*
import com.stark.shoot.infrastructure.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ChatMessageMapper {

    fun toDocument(domain: ChatMessage): ChatMessageDocument {
        return ChatMessageDocument(
            roomId = domain.roomId.toObjectId(),
            senderId = domain.senderId.toObjectId(),
            content = toMessageContentDocument(domain.content),
            status = domain.status,
            replyToMessageId = domain.replyToMessageId?.let { ObjectId(it) },
            reactions = domain.reactions.mapValues { (_, userIds) ->
                userIds.map { ObjectId(it) }.toSet()
            },
            mentions = domain.mentions.map { ObjectId(it) }.toSet(),
            isDeleted = domain.isDeleted,
            readBy = domain.readBy.toMutableMap(),
            metadata = domain.metadata.toMutableMap(), // 메타데이터 필드
            isPinned = domain.isPinned,
            pinnedBy = domain.pinnedBy?.let { ObjectId(it) },
            pinnedAt = domain.pinnedAt
        ).apply {
            // BaseMongoDocument의 id를 나중에 설정
            id = domain.id?.let { ObjectId(it) }
            createdAt = domain.createdAt
        }
    }

    fun toDomain(document: ChatMessageDocument): ChatMessage {
        return ChatMessage(
            id = document.id?.toString(),          // ObjectId -> String
            roomId = document.roomId.toString(),   // ObjectId -> String
            senderId = document.senderId.toString(), // ObjectId -> String
            content = toMessageContent(document.content),
            status = document.status,
            replyToMessageId = document.replyToMessageId?.toString(),
            reactions = document.reactions.mapValues { (_, userIds) ->  // Set<ObjectId> -> Set<String>
                userIds.map { it.toString() }.toSet()
            },
            mentions = document.mentions.map { it.toString() }.toSet(),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
            readBy = document.readBy.toMutableMap(),
            metadata = document.metadata.toMutableMap() ?: mutableMapOf(), // 메타데이터 필드
            isPinned = document.isPinned,
            pinnedBy = document.pinnedBy?.toString(),
            pinnedAt = document.pinnedAt
        )
    }

    // MessageContent <-> MessageContentDocument
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

    // MessageMetadata <-> MessageMetadataDocument
    private fun toMessageMetadataDocument(domain: MessageMetadata): MessageMetadataDocument {
        return MessageMetadataDocument(
            urlPreview = domain.urlPreview?.let { toUrlPreviewDocument(it) },
            readAt = domain.readAt
        )
    }

    private fun toMessageMetadata(document: MessageMetadataDocument): MessageMetadata {
        return MessageMetadata(
            urlPreview = document.urlPreview?.let { toUrlPreview(it) },
            readAt = document.readAt
        )
    }

    // 첨부 파일 변환 메서드들
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

    // URL 프리뷰 변환 메서드들
    private fun toUrlPreviewDocument(domain: UrlPreview): UrlPreviewDocument {
        return UrlPreviewDocument(
            url = domain.url,
            title = domain.title,
            description = domain.description,
            imageUrl = domain.imageUrl,
            siteName = domain.siteName,
            fetchedAt = domain.fetchedAt
        )
    }

    private fun toUrlPreview(document: UrlPreviewDocument): UrlPreview {
        return UrlPreview(
            url = document.url,
            title = document.title,
            description = document.description,
            imageUrl = document.imageUrl,
            siteName = document.siteName,
            fetchedAt = document.fetchedAt
        )
    }

}