package com.stark.shoot.domain.chatroom.vo

/**
 * MessageId Value Object (ChatRoom Context)
 *
 * ChatRoom Context에서 사용하는 메시지 ID
 * - Chat Context의 MessageId와 구조적으로 동일하지만 타입은 다름
 * - ACL을 통해 Chat Context의 MessageId와 변환
 * - Context 간 독립성 유지
 */
@JvmInline
value class MessageId private constructor(val value: String) {
    companion object {
        fun from(value: String): MessageId {
            require(value.isNotBlank()) { "메시지 ID는 비어있을 수 없습니다." }
            return MessageId(value)
        }
    }

    override fun toString(): String = value
}
