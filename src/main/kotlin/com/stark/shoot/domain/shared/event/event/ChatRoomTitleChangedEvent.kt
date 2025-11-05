package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 채팅방 제목 변경 이벤트
 *
 * @property version Event schema version for MSA compatibility
 */
data class ChatRoomTitleChangedEvent(
    val version: String = "1.0",
    val roomId: ChatRoomId,
    val oldTitle: String?,
    val newTitle: String,
    val changedBy: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent