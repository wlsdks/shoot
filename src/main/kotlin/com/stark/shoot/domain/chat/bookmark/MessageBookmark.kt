package com.stark.shoot.domain.chat.bookmark

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 사용자 개인의 메시지 북마크 정보를 나타내는 애그리게이트
 */
data class MessageBookmark(
    val id: String? = null,
    val messageId: MessageId,
    val userId: UserId,
    val createdAt: Instant = Instant.now(),
)
