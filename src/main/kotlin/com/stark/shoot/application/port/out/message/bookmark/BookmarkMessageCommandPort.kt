package com.stark.shoot.application.port.out.message.bookmark

import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

interface BookmarkMessageCommandPort {
    fun saveBookmark(bookmark: MessageBookmark): MessageBookmark
    fun deleteBookmark(messageId: MessageId, userId: UserId)
}