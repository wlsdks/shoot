package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

interface BookmarkMessagePort {
    fun saveBookmark(bookmark: MessageBookmark): MessageBookmark
    fun deleteBookmark(messageId: MessageId, userId: UserId)
    fun findBookmarksByUser(userId: UserId, roomId: ChatRoomId? = null): List<MessageBookmark>
    fun exists(messageId: MessageId, userId: UserId): Boolean
}
