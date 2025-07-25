package com.stark.shoot.application.port.out.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface BookmarkMessageQueryPort {
    fun findBookmarksByUser(userId: UserId, roomId: ChatRoomId? = null): List<MessageBookmark>
    fun exists(messageId: MessageId, userId: UserId): Boolean
}