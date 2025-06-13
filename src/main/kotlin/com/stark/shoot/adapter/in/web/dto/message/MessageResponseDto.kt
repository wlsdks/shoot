package com.stark.shoot.adapter.`in`.web.dto.message

import com.stark.shoot.domain.chat.message.UrlPreview
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import java.time.Instant

/**
 * 메시지 응답 DTO
 * API 응답으로 사용되는 메시지 데이터 객체
 */
@ApplicationDto
data class MessageResponseDto(
    val id: String,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContentResponseDto,  // content 객체 구조로 변경
    val status: MessageStatus,
    val threadId: String? = null,
    val replyToMessageId: String? = null,
    val reactions: Map<String, Set<Long>> = emptyMap(),
    val mentions: Set<Long> = emptySet(),
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val readBy: Map<Long, Boolean> = emptyMap(),
    val isPinned: Boolean = false,
    val pinnedBy: Long? = null,
    val pinnedAt: Instant? = null
)

/**
 * 메시지 내 실제 컨텐츠 객체 DTO
 */
@ApplicationDto
data class MessageContentResponseDto(
    val text: String,
    val type: MessageType,
    val attachments: List<AttachmentDto> = emptyList(),
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val urlPreview: UrlPreviewDto? = null,
)

/**
 * 메시지 내 실제 컨텐츠 객체 DTO
 */
@ApplicationDto
data class MessageMetadataResponseDto(
    val tempId: String? = null,
    val needsUrlPreview: Boolean = false,
    val previewUrl: String? = null,
    val urlPreview: UrlPreview? = null,
    var readAt: Instant? = null
)

/**
 * 첨부파일 DTO
 */
@ApplicationDto
data class AttachmentDto(
    val id: String,
    val filename: String,
    val contentType: String,
    val size: Long,
    val url: String,
    val thumbnailUrl: String? = null
)

/**
 * URL 프리뷰 DTO
 */
@ApplicationDto
data class UrlPreviewDto(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String? = null
)
