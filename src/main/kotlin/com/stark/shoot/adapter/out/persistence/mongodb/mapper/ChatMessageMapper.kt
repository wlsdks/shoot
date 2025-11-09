package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.rest.dto.message.AttachmentDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.UrlPreviewDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.AttachmentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ChatMessageMapper {

    fun toDocument(domain: ChatMessage): ChatMessageDocument {
        return ChatMessageDocument(
            roomId = domain.roomId.value,
            senderId = domain.senderId.value,
            content = toMessageContentDocument(domain.content),
            status = domain.status,
            threadId = domain.threadId?.let { ObjectId(it.value) },
            replyToMessageId = domain.replyToMessageId?.let { ObjectId(it.value) },
            reactions = emptyMap(),  // 리액션은 별도 Aggregate로 관리
            mentions = domain.mentions.map { it.value }.toSet(),
            isDeleted = domain.isDeleted
            // 메시지 고정 정보는 별도 MessagePin Aggregate로 관리
            // 메시지 읽음 표시는 별도 MessageReadReceipt Aggregate로 관리
        ).apply {
            id = domain.id?.let { ObjectId(it.value) }
            createdAt = domain.createdAt
        }
    }

    fun toDomain(document: ChatMessageDocument): ChatMessage {
        return ChatMessage(
            id = document.id?.toString()?.let { MessageId.from(it) },
            roomId = ChatRoomId.from(document.roomId).toChat(),
            senderId = UserId.from(document.senderId),
            content = toMessageContent(document.content),
            status = document.status,
            threadId = document.threadId?.toString()?.let { MessageId.from(it) },
            replyToMessageId = document.replyToMessageId?.toString()?.let { MessageId.from(it) },
            // reactions는 별도 Aggregate로 관리 (MessageReaction)
            mentions = document.mentions.map { UserId.from(it) }.toSet(),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
            // 메시지 고정 정보는 별도 MessagePin Aggregate로 관리
            // 메시지 읽음 표시는 별도 MessageReadReceipt Aggregate로 관리
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
    private fun toAttachmentDocument(domain: MessageContent.Attachment): AttachmentDocument {
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

    private fun toAttachment(document: AttachmentDocument): MessageContent.Attachment {
        return MessageContent.Attachment(
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
    private fun toAttachmentDto(attachment: MessageContent.Attachment): AttachmentDto {
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
            id = message.id?.value ?: "",
            roomId = message.roomId.value,
            senderId = message.senderId.value,
            content = MessageContentResponseDto(
                text = message.content.text,
                type = message.content.type,
                attachments = message.content.attachments.map { toAttachmentDto(it) },
                isEdited = message.content.isEdited,
                isDeleted = message.content.isDeleted,
                urlPreview = message.content.metadata?.urlPreview?.let { toUrlPreviewDto(it) }
            ),
            status = message.status,
            threadId = message.threadId?.value,
            replyToMessageId = message.replyToMessageId?.value,
            reactions = emptyMap(),  // 리액션은 별도 Aggregate로 관리 (MessageReaction)
            mentions = message.mentions.map { it.value }.toSet(),
            createdAt = message.createdAt,
            updatedAt = message.updatedAt
            // 메시지 고정 정보는 별도 MessagePin Aggregate로 관리
            // 메시지 읽음 표시는 별도 MessageReadReceipt Aggregate로 관리
        )
    }

    // UrlPreview 도메인 객체를 DTO로 변환하는 매퍼
    private fun toUrlPreviewDto(preview: ChatMessageMetadata.UrlPreview): UrlPreviewDto {
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
