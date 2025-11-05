package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 메시지 전송 이벤트
 *
 * DDD 개선: ChatMessage 도메인 객체 제거, primitive 타입과 VO만 사용
 */
data class MessageSentEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val senderId: UserId,
    val content: String,
    val type: MessageType,
    val mentions: Set<UserId>,
    val createdAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {
    companion object {
        fun create(message: com.stark.shoot.domain.chat.message.ChatMessage): MessageSentEvent {
            return MessageSentEvent(
                messageId = message.id ?: throw IllegalStateException("Message ID must not be null"),
                roomId = message.roomId,
                senderId = message.senderId,
                content = message.content.text,
                type = message.content.type,
                mentions = message.mentions,
                createdAt = message.createdAt ?: Instant.now()
            )
        }
    }
}
