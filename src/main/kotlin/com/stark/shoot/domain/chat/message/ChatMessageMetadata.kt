package com.stark.shoot.domain.chat.message

// Domain model should not depend on external adapter classes
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
}
