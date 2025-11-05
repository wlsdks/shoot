package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 채팅방 참여자 변경 이벤트
 */
data class ChatRoomParticipantChangedEvent(
    val roomId: ChatRoomId,
    val participantsAdded: Set<UserId>,
    val participantsRemoved: Set<UserId>,
    val changedBy: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent