package com.stark.shoot.application.port.`in`.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId

interface BookmarkMessageUseCase {
    fun bookmarkMessage(messageId: MessageId, userId: UserId): MessageBookmark
    fun removeBookmark(messageId: MessageId, userId: UserId)
    fun getBookmarks(userId: UserId, roomId: ChatRoomId? = null): List<MessageBookmark>
}
