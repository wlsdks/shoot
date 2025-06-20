package com.stark.shoot.domain.event

import com.stark.shoot.domain.event.DomainEvent

/**
 * 채팅방의 각 참여자의 unreadCount 업데이트 이벤트.
 *
 * @param roomId 채팅방 ID
 * @param unreadCounts 각 참여자(문자열 ID)별 읽지 않은 메시지 수
 */
data class MessageUnreadCountUpdatedEvent(
    val roomId: Long,
    val unreadCounts: Map<Long, Int>,
    val lastMessage: String? = null,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 읽지 않은 메시지 수 업데이트 이벤트 생성
         *
         * @param roomId 채팅방 ID
         * @param unreadCounts 각 참여자별 읽지 않은 메시지 수
         * @param lastMessage 마지막 메시지 내용 (선택)
         * @return 생성된 MessageUnreadCountUpdatedEvent 객체
         */
        fun create(
            roomId: Long,
            unreadCounts: Map<Long, Int>,
            lastMessage: String? = null
        ): MessageUnreadCountUpdatedEvent {
            return MessageUnreadCountUpdatedEvent(
                roomId = roomId,
                unreadCounts = unreadCounts,
                lastMessage = lastMessage
            )
        }
    }
}
