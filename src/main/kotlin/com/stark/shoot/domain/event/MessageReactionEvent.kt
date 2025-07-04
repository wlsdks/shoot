package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

data class MessageReactionEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val userId: UserId,
    val reactionType: String,
    val isAdded: Boolean,  // true: 추가, false: 제거
    val isReplacement: Boolean = false,  // true: 리액션 교체의 일부, false: 일반 추가/제거
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        /**
         * 메시지 반응 이벤트 생성
         *
         * @param messageId 메시지 ID
         * @param roomId 채팅방 ID
         * @param userId 사용자 ID
         * @param reactionType 리액션 타입
         * @param isAdded 추가 여부 (true: 추가, false: 제거)
         * @param isReplacement 리액션 교체의 일부 여부 (true: 리액션 교체의 일부, false: 일반 추가/제거)
         * @return 생성된 MessageReactionEvent 객체
         */
        fun create(
            messageId: MessageId,
            roomId: ChatRoomId,
            userId: UserId,
            reactionType: String,
            isAdded: Boolean,
            isReplacement: Boolean = false
        ): MessageReactionEvent {
            return MessageReactionEvent(
                messageId = messageId,
                roomId = roomId,
                userId = userId,
                reactionType = reactionType,
                isAdded = isAdded,
                isReplacement = isReplacement
            )
        }
    }
}
