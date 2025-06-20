package com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "message_bookmarks")
data class MessageBookmarkDocument(
    @Id
    val id: String? = null,
    @Indexed
    val messageId: String,
    @Indexed
    val userId: Long,
    val createdAt: Instant = Instant.now(),
) {
    fun toDomain(): MessageBookmark {
        return MessageBookmark(
            id = id,
            messageId = MessageId.from(messageId),
            userId = UserId.from(userId),
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(bookmark: MessageBookmark): MessageBookmarkDocument {
            return MessageBookmarkDocument(
                id = bookmark.id,
                messageId = bookmark.messageId.value,
                userId = bookmark.userId.value,
                createdAt = bookmark.createdAt
            )
        }
    }
}
