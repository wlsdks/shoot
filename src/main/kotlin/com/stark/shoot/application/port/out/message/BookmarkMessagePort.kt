package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface BookmarkMessagePort {
    fun saveBookmark(bookmark: MessageBookmark): MessageBookmark
    fun deleteBookmark(messageId: MessageId, userId: UserId)
    fun findBookmarksByUser(userId: UserId, roomId: Long? = null): List<MessageBookmark>
    fun exists(messageId: MessageId, userId: UserId): Boolean
}
