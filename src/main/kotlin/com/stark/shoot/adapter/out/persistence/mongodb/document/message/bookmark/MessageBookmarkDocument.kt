package com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
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
            messageId = messageId,
            userId = userId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(bookmark: MessageBookmark): MessageBookmarkDocument {
            return MessageBookmarkDocument(
                id = bookmark.id,
                messageId = bookmark.messageId,
                userId = bookmark.userId,
                createdAt = bookmark.createdAt
            )
        }
    }
}
