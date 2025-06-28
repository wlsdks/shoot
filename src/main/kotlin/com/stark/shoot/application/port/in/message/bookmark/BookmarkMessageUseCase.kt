package com.stark.shoot.application.port.`in`.message.bookmark

import com.stark.shoot.application.port.`in`.message.bookmark.command.BookmarkMessageCommand
import com.stark.shoot.application.port.`in`.message.bookmark.command.GetBookmarksCommand
import com.stark.shoot.application.port.`in`.message.bookmark.command.RemoveBookmarkCommand
import com.stark.shoot.domain.chat.bookmark.MessageBookmark

interface BookmarkMessageUseCase {
    fun bookmarkMessage(command: BookmarkMessageCommand): MessageBookmark
    fun removeBookmark(command: RemoveBookmarkCommand)
    fun getBookmarks(command: GetBookmarksCommand): List<MessageBookmark>
}
