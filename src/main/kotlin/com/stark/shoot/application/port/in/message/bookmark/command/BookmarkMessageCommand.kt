package com.stark.shoot.application.port.`in`.message.bookmark.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 북마크 커맨드
 *
 * @property messageId 북마크할 메시지 ID
 * @property userId 북마크를 생성하는 사용자 ID
 */
data class BookmarkMessageCommand(
    val messageId: MessageId,
    val userId: UserId
)