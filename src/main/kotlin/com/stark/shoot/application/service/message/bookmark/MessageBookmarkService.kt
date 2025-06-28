package com.stark.shoot.application.service.message.bookmark

import com.stark.shoot.application.port.`in`.message.bookmark.BookmarkMessageUseCase
import com.stark.shoot.application.port.`in`.message.bookmark.command.BookmarkMessageCommand
import com.stark.shoot.application.port.`in`.message.bookmark.command.GetBookmarksCommand
import com.stark.shoot.application.port.`in`.message.bookmark.command.RemoveBookmarkCommand
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.bookmark.BookmarkMessageCommandPort
import com.stark.shoot.application.port.out.message.bookmark.BookmarkMessageQueryPort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class MessageBookmarkService(
    private val bookmarkCommandPort: BookmarkMessageCommandPort,
    private val bookmarkQueryPort: BookmarkMessageQueryPort,
    private val loadMessagePort: LoadMessagePort,
) : BookmarkMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun bookmarkMessage(command: BookmarkMessageCommand): MessageBookmark {
        val messageId = command.messageId
        val userId = command.userId

        if (bookmarkQueryPort.exists(messageId, userId)) {
            logger.debug { "이미 북마크된 메시지입니다: messageId=$messageId, userId=$userId" }
            throw IllegalArgumentException("이미 북마크된 메시지입니다.")
        }

        loadMessagePort.findById(messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=$messageId")

        val bookmark = MessageBookmark(
            messageId = messageId,
            userId = userId,
            createdAt = Instant.now(),
        )

        return bookmarkCommandPort.saveBookmark(bookmark)
    }

    override fun removeBookmark(command: RemoveBookmarkCommand) {
        bookmarkCommandPort.deleteBookmark(command.messageId, command.userId)
    }

    override fun getBookmarks(command: GetBookmarksCommand): List<MessageBookmark> {
        return bookmarkQueryPort.findBookmarksByUser(command.userId, command.roomId)
    }

}
