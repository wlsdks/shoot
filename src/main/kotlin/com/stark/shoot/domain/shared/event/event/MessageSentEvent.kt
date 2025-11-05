package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 메시지 전송 이벤트
 *
 * DDD 개선:
 * - ChatMessage 도메인 객체 제거, primitive 타입과 VO만 사용
 * - Factory method 제거: Shared Kernel이 특정 Context에 의존하지 않도록 함
 * - MSA 준비: Event는 모든 서비스에서 독립적으로 사용 가능해야 함
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
) : DomainEvent
