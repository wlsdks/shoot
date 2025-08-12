package com.stark.shoot.domain.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * 채팅방 제목 변경 이벤트
 */
data class ChatRoomTitleChangedEvent(
    val roomId: ChatRoomId,
    val oldTitle: String?,
    val newTitle: String,
    val changedBy: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent