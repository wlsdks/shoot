package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 채팅방 제목 변경 이벤트
 *
 * @property version Event schema version for MSA compatibility
 */
data class ChatRoomTitleChangedEvent(
    override val version: EventVersion = EventVersion.CHATROOM_TITLE_CHANGED_V1,
    val roomId: ChatRoomId,
    val oldTitle: String?,
    val newTitle: String,
    val changedBy: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent