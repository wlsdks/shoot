package com.stark.shoot.application.port.`in`.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark

interface BookmarkMessageUseCase {
    fun bookmarkMessage(messageId: String, userId: Long): MessageBookmark
    fun removeBookmark(messageId: String, userId: Long)
    fun getBookmarks(userId: Long, roomId: Long? = null): List<MessageBookmark>
}
