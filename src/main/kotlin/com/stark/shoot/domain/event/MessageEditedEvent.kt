package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 메시지 수정 이벤트
 * 메시지가 수정되었을 때 발행되는 도메인 이벤트
 */
data class MessageEditedEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val userId: UserId,
    val oldContent: String,
    val newContent: String,
    val editedAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 수정 이벤트 생성
         *
         * @param messageId 수정된 메시지 ID
         * @param roomId 채팅방 ID
         * @param userId 수정한 사용자 ID
         * @param oldContent 이전 메시지 내용
         * @param newContent 새 메시지 내용
         * @param editedAt 수정 시간
         * @return 생성된 MessageEditedEvent 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            userId: UserId,
            oldContent: String,
            newContent: String,
            editedAt: Instant = Instant.now()
        ): MessageEditedEvent {
            return MessageEditedEvent(
                messageId = messageId,
                roomId = roomId,
                userId = userId,
                oldContent = oldContent,
                newContent = newContent,
                editedAt = editedAt
            )
        }
    }
}
