package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 메시지 삭제 이벤트
 * 메시지가 삭제되었을 때 발행되는 도메인 이벤트
 *
 * WebSocket 브로드캐스트는 MongoDB 저장 완료 후에 수행되도록
 * @TransactionalEventListener에서 처리됩니다.
 *
 * DDD 개선: ChatMessage 도메인 객체 제거, primitive 타입과 VO만 사용
 */
data class MessageDeletedEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val userId: UserId,
    val deletedAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 삭제 이벤트 생성
         *
         * @param messageId 삭제된 메시지 ID
         * @param roomId 채팅방 ID
         * @param userId 삭제한 사용자 ID
         * @param deletedAt 삭제 시간
         * @return 생성된 MessageDeletedEvent 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            userId: UserId,
            deletedAt: Instant = Instant.now()
        ): MessageDeletedEvent {
            return MessageDeletedEvent(
                messageId = messageId,
                roomId = roomId,
                userId = userId,
                deletedAt = deletedAt
            )
        }
    }
}
