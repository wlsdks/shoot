package com.stark.shoot.application.port.`in`.message.bookmark.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

/**
 * 메시지 북마크 제거 커맨드
 *
 * @property messageId 북마크를 제거할 메시지 ID
 * @property userId 북마크를 제거하는 사용자 ID
 */
data class RemoveBookmarkCommand(
    val messageId: MessageId,
    val userId: UserId
)