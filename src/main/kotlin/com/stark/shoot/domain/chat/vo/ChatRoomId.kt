package com.stark.shoot.domain.chat.vo

/**
 * Chat Context의 ChatRoomId
 *
 * DDD 개선: ChatRoom Context와의 의존성 제거
 * - Chat Context는 자체 ChatRoomId VO를 가짐
 * - MSA 환경에서 각 Context/서비스는 독립적인 타입 시스템을 가져야 함
 * - ChatRoom Context의 ChatRoomId와 구조적으로 동일하지만, 타입은 다름
 * - 필요시 Anti-Corruption Layer에서 변환 처리
 */
@JvmInline
value class ChatRoomId private constructor(val value: Long) {

    companion object {
        fun from(value: Long): ChatRoomId {
            require(value > 0) { "채팅방 ID는 양수여야 합니다." }
            return ChatRoomId(value)
        }
    }

    override fun toString(): String = value.toString()

}
