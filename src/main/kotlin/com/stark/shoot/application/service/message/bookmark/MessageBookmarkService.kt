package com.stark.shoot.application.service.message.bookmark

import com.stark.shoot.application.port.`in`.message.bookmark.BookmarkMessageUseCase
import com.stark.shoot.application.port.out.message.BookmarkMessagePort
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class MessageBookmarkService(
    private val bookmarkPort: BookmarkMessagePort,
    private val loadMessagePort: LoadMessagePort,
) : BookmarkMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun bookmarkMessage(messageId: String, userId: Long): MessageBookmark {
        if (bookmarkPort.exists(messageId, userId)) {
            logger.debug { "이미 북마크된 메시지입니다: messageId=$messageId, userId=$userId" }
            throw IllegalArgumentException("이미 북마크된 메시지입니다.")
        }

        loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        val bookmark = MessageBookmark(
            messageId = messageId,
            userId = userId,
            createdAt = Instant.now(),
        )
        return bookmarkPort.saveBookmark(bookmark)
    }

    override fun removeBookmark(messageId: String, userId: Long) {
        bookmarkPort.deleteBookmark(messageId, userId)
    }

    override fun getBookmarks(userId: Long, roomId: Long?): List<MessageBookmark> {
        return bookmarkPort.findBookmarksByUser(userId, roomId)
    }
}
