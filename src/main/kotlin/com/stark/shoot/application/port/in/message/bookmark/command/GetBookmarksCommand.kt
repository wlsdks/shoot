package com.stark.shoot.application.port.`in`.message.bookmark.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 북마크 목록 조회 커맨드
 *
 * @property userId 북마크를 조회할 사용자 ID
 * @property roomId 특정 채팅방의 북마크만 조회할 경우 채팅방 ID (null이면 모든 채팅방의 북마크 조회)
 */
data class GetBookmarksCommand(
    val userId: UserId,
    val roomId: ChatRoomId? = null
)