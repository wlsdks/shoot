package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ScheduledMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Component

/**
 * ScheduledMessage 도메인과 ScheduledMessageDocument 간의 변환을 담당하는 매퍼
 */
@Component
class ScheduledMessageMapper() {
    /**
     * 도메인 모델을 문서로 변환
     */
    fun toDocument(domain: ScheduledMessage): ScheduledMessageDocument {
        return ScheduledMessageDocument(
            roomId = domain.roomId,
            senderId = domain.senderId,
            content = toMessageContentDocument(domain.content),
            scheduledAt = domain.scheduledAt,
            status = domain.status.name,
            metadata = domain.metadata.toMessageMetadataDocument(),
        ).apply {
            id = domain.id?.toString()?.toObjectId()
            createdAt = domain.createdAt
        }
    }

    /**
     * 문서를 도메인 모델로 변환
     */
    fun toDomain(document: ScheduledMessageDocument): ScheduledMessage {
        return ScheduledMessage(
            id = document.id?.toString()?.let { MessageId.from(it) },
            roomId = document.roomId,
            senderId = document.senderId,
            content = toMessageContent(document.content),
            scheduledAt = document.scheduledAt,
            createdAt = document.createdAt ?: document.scheduledAt.minusSeconds(1),
            status = ScheduledMessageStatus.valueOf(document.status),
            metadata = document.metadata.toMessageMetadata(),
        )
    }

    /**
     * MessageContent를 MessageContentDocument로 변환
     * (ChatMessageMapper의 private 메소드를 재사용할 수 없어 간소화된 버전 구현)
     */
    private fun toMessageContentDocument(domain: MessageContent): MessageContentDocument {
        return MessageContentDocument(
            text = domain.text,
            type = domain.type,
            isEdited = domain.isEdited,
            isDeleted = domain.isDeleted
        )
    }

    /**
     * MessageContentDocument를 MessageContent로 변환
     * (ChatMessageMapper의 private 메소드를 재사용할 수 없어 간소화된 버전 구현)
     */
    private fun toMessageContent(document: MessageContentDocument): MessageContent {
        return MessageContent(
            text = document.text,
            type = document.type,
            isEdited = document.isEdited,
            isDeleted = document.isDeleted
        )
    }

    /**
     * ScheduledMessage를 ScheduledMessageResponseDto로 변환
     */
    fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
        return ScheduledMessageResponseDto(
            id = domain.id!!.toString(),
            roomId = domain.roomId,
            senderId = domain.senderId,
            content = MessageContentResponseDto(
                text = domain.content.text,
                type = domain.content.type,
                isEdited = domain.content.isEdited,
                isDeleted = domain.content.isDeleted
            ),
            scheduledAt = domain.scheduledAt,
            createdAt = domain.createdAt,
            status = domain.status,
            metadata = domain.metadata.toMessageMetadataResponseDto()
        )
    }

}
