package com.stark.shoot.domain.chat.room

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
