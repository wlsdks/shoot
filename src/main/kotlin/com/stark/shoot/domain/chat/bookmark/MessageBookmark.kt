package com.stark.shoot.domain.chat.bookmark

import com.stark.shoot.domain.chat.bookmark.vo.MessageBookmarkId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 사용자 개인의 메시지 북마크 정보를 나타내는 애그리게이트
 */
@AggregateRoot
data class MessageBookmark(
    val id: MessageBookmarkId? = null,
    val messageId: MessageId,
    val userId: UserId,
    var createdAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * 메시지 북마크 생성
         *
         * @param userId 북마크를 생성한 사용자 ID
         * @param messageId 북마크할 메시지 ID
         * @return 생성된 북마크
         */
        fun create(userId: UserId, messageId: MessageId): MessageBookmark {
            return MessageBookmark(
                userId = userId,
                messageId = messageId
            )
        }
    }
}
