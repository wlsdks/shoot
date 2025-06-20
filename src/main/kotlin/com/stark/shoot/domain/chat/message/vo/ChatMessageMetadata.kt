package com.stark.shoot.domain.chat.message.vo

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

    // Jsoup으로 파싱한 URL 미리보기 정보
    data class UrlPreview(
        val url: String,
        val title: String?,
        val description: String?,
        val imageUrl: String?,
        val siteName: String? = null,
        val fetchedAt: Instant = Instant.now()
    )
}