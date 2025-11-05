package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.bookmark

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark.MessageBookmarkDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageBookmarkMongoRepository
import com.stark.shoot.application.port.out.message.bookmark.BookmarkMessageCommandPort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class BookmarkMessageCommandMongoAdapter(
    private val bookmarkRepository: MessageBookmarkMongoRepository
) : BookmarkMessageCommandPort {

    override fun saveBookmark(bookmark: MessageBookmark): MessageBookmark {
        val document = MessageBookmarkDocument.Companion.fromDomain(bookmark)
        val saved = bookmarkRepository.save(document)
        return saved.toDomain()
    }

    override fun deleteBookmark(
        messageId: MessageId,
        userId: UserId
    ) {
        bookmarkRepository.deleteByMessageIdAndUserId(messageId.value, userId.value)
    }

}