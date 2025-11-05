package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.bookmark

import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageBookmarkMongoRepository
import com.stark.shoot.application.port.out.message.bookmark.BookmarkMessageQueryPort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId

@Adapter
class BookmarkMessageQueryMongoAdapter(
    private val bookmarkRepository: MessageBookmarkMongoRepository,
    private val chatMessageRepository: ChatMessageMongoRepository,
) : BookmarkMessageQueryPort {

    override fun findBookmarksByUser(
        userId: UserId,
        roomId: ChatRoomId?
    ): List<MessageBookmark> {
        val documents = bookmarkRepository.findByUserId(userId.value)
        if (roomId == null) {
            return documents.map { it.toDomain() }
        }

        return documents.filter { doc ->
            val message = chatMessageRepository.findById(ObjectId(doc.messageId))
            message.isPresent && message.get().roomId == roomId.value
        }.map { it.toDomain() }
    }

    override fun exists(
        messageId: MessageId,
        userId: UserId
    ): Boolean {
        return bookmarkRepository.existsByMessageIdAndUserId(messageId.value, userId.value)
    }

}