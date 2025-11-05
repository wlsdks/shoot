package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * @property version Event schema version for MSA compatibility
 */
data class MessageBulkReadEvent(
    val version: String = "1.0",
    val roomId: ChatRoomId,
    val messageIds: List<MessageId>,
    val userId: UserId,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 일괄 읽음 이벤트 생성
         *
         * @param roomId 채팅방 ID
         * @param messageIds 읽은 메시지 ID 목록
         * @param userId 사용자 ID
         * @return 생성된 MessageBulkReadEvent 객체
         */
        fun create(
            roomId: ChatRoomId,
            messageIds: List<MessageId>,
            userId: UserId
        ): MessageBulkReadEvent {
            return MessageBulkReadEvent(
                roomId = roomId,
                messageIds = messageIds,
                userId = userId
            )
        }
    }
}
