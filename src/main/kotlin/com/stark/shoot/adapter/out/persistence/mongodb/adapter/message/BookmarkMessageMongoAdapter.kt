package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark.MessageBookmarkDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageBookmarkMongoRepository
import com.stark.shoot.application.port.out.message.BookmarkMessagePort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId

@Adapter
class BookmarkMessageMongoAdapter(
    private val bookmarkRepository: MessageBookmarkMongoRepository,
    private val chatMessageRepository: ChatMessageMongoRepository,
) : BookmarkMessagePort {

    override fun saveBookmark(bookmark: MessageBookmark): MessageBookmark {
        val document = MessageBookmarkDocument.fromDomain(bookmark)
        val saved = bookmarkRepository.save(document)
        return saved.toDomain()
    }

    override fun deleteBookmark(messageId: String, userId: Long) {
        bookmarkRepository.deleteByMessageIdAndUserId(messageId, userId)
    }

    override fun findBookmarksByUser(userId: Long, roomId: Long?): List<MessageBookmark> {
        val documents = bookmarkRepository.findByUserId(userId)
        if (roomId == null) {
            return documents.map { it.toDomain() }
        }

        return documents.filter { doc ->
            val message = chatMessageRepository.findById(ObjectId(doc.messageId))
            message.isPresent && message.get().roomId == roomId
        }.map { it.toDomain() }
    }

    override fun exists(messageId: String, userId: Long): Boolean {
        return bookmarkRepository.existsByMessageIdAndUserId(messageId, userId)
    }
}
