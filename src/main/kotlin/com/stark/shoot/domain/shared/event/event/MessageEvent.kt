package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.shared.event.type.EventType
import java.time.Instant

/**
 * 메시지 이벤트
 *
 * DDD 개선:
 * - ChatMessage 도메인 객체 제거, primitive 타입과 VO만 사용
 * - Factory method 제거: Shared Kernel이 특정 Context에 의존하지 않도록 함
 * - Kafka를 통해 전송되는 이벤트로, 직렬화 효율성과 Context 독립성이 중요
 * - MSA 준비: Event는 모든 서비스에서 독립적으로 사용 가능해야 함
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
)
