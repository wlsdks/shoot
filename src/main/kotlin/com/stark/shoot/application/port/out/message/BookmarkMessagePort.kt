package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.bookmark.MessageBookmark

interface BookmarkMessagePort {
    fun saveBookmark(bookmark: MessageBookmark): MessageBookmark
    fun deleteBookmark(messageId: String, userId: Long)
    fun findBookmarksByUser(userId: Long, roomId: Long? = null): List<MessageBookmark>
    fun exists(messageId: String, userId: Long): Boolean
}
