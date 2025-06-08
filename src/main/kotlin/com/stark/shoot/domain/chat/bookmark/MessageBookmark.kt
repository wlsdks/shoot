package com.stark.shoot.domain.chat.bookmark

import java.time.Instant

/**
 * 사용자 개인의 메시지 북마크 정보를 나타내는 애그리게이트
 */
data class MessageBookmark(
    val id: String? = null,
    val messageId: String,
    val userId: Long,
    val createdAt: Instant = Instant.now(),
)
