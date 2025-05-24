package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageMetadataRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageMetadataResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import java.time.Instant

data class ChatMessageMetadata(
    val tempId: String? = null,
    val needsUrlPreview: Boolean = false,
    val previewUrl: String? = null,
    val urlPreview: UrlPreview? = null,
    var readAt: Instant? = null
) {
    companion object {
        /**
         * 요청 객체로부터 메타데이터 생성
         *
         * @param request 메타데이터 요청 객체
         * @return 생성된 ChatMessageMetadata 객체
         */
        fun fromRequest(request: ChatMessageMetadataRequest): ChatMessageMetadata {
            return ChatMessageMetadata(
                tempId = request.tempId,
                needsUrlPreview = request.needsUrlPreview,
                previewUrl = request.previewUrl,
                urlPreview = request.urlPreview?.let {
                    UrlPreview(
                        url = it.url,
                        title = it.title,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        siteName = it.siteName
                    )
                }
            )
        }

        /**
         * 기본 메타데이터 생성
         *
         * @param tempId 임시 ID (선택)
         * @return 생성된 ChatMessageMetadata 객체
         */
        fun create(tempId: String? = null): ChatMessageMetadata {
            return ChatMessageMetadata(
                tempId = tempId
            )
        }
    }

    fun toMessageMetadataDocument(): MessageMetadataDocument {
        return MessageMetadataDocument(
            tempId = tempId,
            needsUrlPreview = needsUrlPreview,
            previewUrl = previewUrl,
            urlPreview = urlPreview
        )
    }

    fun toMessageMetadataResponseDto(): MessageMetadataResponseDto {
        return MessageMetadataResponseDto(
            tempId = tempId,
            needsUrlPreview = needsUrlPreview,
            previewUrl = previewUrl,
            urlPreview = urlPreview
        )
    }

    fun toRequestDto(): ChatMessageMetadataRequest {
        return ChatMessageMetadataRequest(
            tempId = tempId,
            needsUrlPreview = needsUrlPreview,
            previewUrl = previewUrl,
            urlPreview = urlPreview?.let {
                UrlPreview(
                    url = it.url,
                    title = it.title,
                    description = it.description,
                    imageUrl = it.imageUrl,
                    siteName = it.siteName
                )
            }
        )
    }
}
