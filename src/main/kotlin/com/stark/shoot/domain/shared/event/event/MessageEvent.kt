package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.shared.event.type.EventType
import java.time.Instant

/**
 * 메시지 이벤트
 *
 * DDD 개선: ChatMessage 도메인 객체 제거, primitive 타입과 VO만 사용
 * Kafka를 통해 전송되는 이벤트로, 직렬화 효율성과 Context 독립성이 중요
 */
data class MessageEvent(
    val version: String = "1.0",
    val type: EventType,
    val messageId: MessageId?,
    val roomId: ChatRoomId,
    val senderId: UserId,
    val content: String,
    val messageType: MessageType,
    val mentions: Set<UserId>,
    val tempId: String?,
    val needsUrlPreview: Boolean,
    val previewUrl: String?,
    val createdAt: Instant,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        /**
         * ChatMessage로부터 MessageEvent 객체를 생성합니다.
         *
         * @param message ChatMessage
         * @param eventType 이벤트 타입 (기본값: MESSAGE_CREATED)
         * @return MessageEvent
         */
        fun fromMessage(
            message: com.stark.shoot.domain.chat.message.ChatMessage,
            eventType: EventType = EventType.MESSAGE_CREATED
        ): MessageEvent {
            return MessageEvent(
                type = eventType,
                messageId = message.id,
                roomId = message.roomId,
                senderId = message.senderId,
                content = message.content.text,
                messageType = message.content.type,
                mentions = message.mentions,
                tempId = message.metadata.tempId,
                needsUrlPreview = message.metadata.needsUrlPreview,
                previewUrl = message.metadata.previewUrl,
                createdAt = message.createdAt ?: Instant.now()
            )
        }
    }
}
