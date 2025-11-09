package com.stark.shoot.domain.chat.pin

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 메시지 고정 Aggregate Root
 *
 * ChatMessage와 독립적으로 메시지 고정 정보를 관리합니다.
 * 각 채팅방에는 최대 N개의 고정 메시지만 존재할 수 있습니다.
 *
 * @property id 메시지 고정 ID (nullable for new entities)
 * @property messageId 고정된 메시지 ID (ID reference to ChatMessage)
 * @property roomId 채팅방 ID (ID reference to ChatRoom)
 * @property pinnedBy 고정한 사용자 ID (ID reference to User)
 * @property pinnedAt 고정된 시각
 * @property createdAt 생성 시각
 */
@AggregateRoot
data class MessagePin(
    val id: MessagePinId? = null,
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val pinnedBy: UserId,
    val pinnedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now()
) {
    companion object {
        /**
         * 새로운 메시지 고정 생성
         *
         * @param messageId 고정할 메시지 ID
         * @param roomId 채팅방 ID
         * @param pinnedBy 고정한 사용자 ID
         * @return 생성된 MessagePin 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            pinnedBy: UserId
        ): MessagePin {
            return MessagePin(
                messageId = messageId,
                roomId = roomId,
                pinnedBy = pinnedBy
            )
        }
    }
}
