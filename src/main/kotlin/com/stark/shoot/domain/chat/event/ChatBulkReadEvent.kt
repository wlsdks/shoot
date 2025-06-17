package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.common.DomainEvent

data class ChatBulkReadEvent(
    val roomId: Long,
    val messageIds: List<String>,
    val userId: Long,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 일괄 읽음 이벤트 생성
         *
         * @param roomId 채팅방 ID
         * @param messageIds 읽은 메시지 ID 목록
         * @param userId 사용자 ID
         * @return 생성된 ChatBulkReadEvent 객체
         */
        fun create(
            roomId: Long,
            messageIds: List<String>,
            userId: Long
        ): ChatBulkReadEvent {
            return ChatBulkReadEvent(
                roomId = roomId,
                messageIds = messageIds,
                userId = userId
            )
        }
    }
}
