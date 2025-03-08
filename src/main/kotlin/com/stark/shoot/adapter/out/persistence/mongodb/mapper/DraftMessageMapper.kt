package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.web.dto.message.draft.DraftMessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.DraftMessageDocument
import com.stark.shoot.domain.chat.message.DraftMessage
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * DraftMessage 도메인과 DraftMessageDocument 간의 변환을 담당하는 매퍼
 */
@Component
class DraftMessageMapper {
    /**
     * 도메인 모델을 문서로 변환
     */
    fun toDocument(domain: DraftMessage): DraftMessageDocument {
        return DraftMessageDocument(
            userId = domain.userId.toObjectId(),
            roomId = domain.roomId.toObjectId(),
            content = domain.content,
            attachments = domain.attachments,
            mentions = domain.mentions,
            metadata = domain.metadata
        ).apply {
            id = domain.id?.let { it.toObjectId() }
            createdAt = domain.createdAt
            updatedAt = domain.updatedAt
        }
    }

    /**
     * 문서를 도메인 모델로 변환
     */
    fun toDomain(document: DraftMessageDocument): DraftMessage {
        return DraftMessage(
            id = document.id?.toString(),
            userId = document.userId.toString(),
            roomId = document.roomId.toString(),
            content = document.content,
            attachments = document.attachments,
            mentions = document.mentions,
            createdAt = document.createdAt ?: Instant.now(),
            updatedAt = document.updatedAt,
            metadata = document.metadata.toMutableMap()
        )
    }

    /**
     * 도메인을 응답 DTO로 변환
     */
    fun toDraftMessageResponseDto(domain: DraftMessage): DraftMessageResponseDto {
        return DraftMessageResponseDto(
            id = domain.id,
            userId = domain.userId,
            roomId = domain.roomId,
            content = domain.content,
            attachments = domain.attachments,
            mentions = domain.mentions,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            metadata = domain.metadata
        )
    }

}