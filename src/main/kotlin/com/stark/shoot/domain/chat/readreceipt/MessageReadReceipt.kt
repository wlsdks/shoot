package com.stark.shoot.domain.chat.readreceipt

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 메시지 읽음 표시 Aggregate Root
 *
 * ChatMessage와 독립적으로 메시지 읽음 정보를 관리합니다.
 * 각 사용자가 메시지를 읽은 시각을 추적합니다.
 *
 * @property id 읽음 표시 ID (nullable for new entities)
 * @property messageId 읽은 메시지 ID (ID reference to ChatMessage)
 * @property roomId 채팅방 ID (ID reference to ChatRoom)
 * @property userId 읽은 사용자 ID (ID reference to User)
 * @property readAt 읽은 시각
 * @property createdAt 생성 시각
 */
@AggregateRoot
data class MessageReadReceipt(
    val id: MessageReadReceiptId? = null,
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val userId: UserId,
    val readAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now()
) {
    companion object {
        /**
         * 새로운 메시지 읽음 표시 생성
         *
         * @param messageId 읽은 메시지 ID
         * @param roomId 채팅방 ID
         * @param userId 읽은 사용자 ID
         * @return 생성된 MessageReadReceipt 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            userId: UserId
        ): MessageReadReceipt {
            return MessageReadReceipt(
                messageId = messageId,
                roomId = roomId,
                userId = userId
            )
        }
    }
}
