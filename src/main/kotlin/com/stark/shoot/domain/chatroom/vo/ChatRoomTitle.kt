package com.stark.shoot.domain.chatroom.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class ChatRoomTitle private constructor(val value: String) {
    companion object {
        fun from(value: String): ChatRoomTitle {
            require(value.isNotBlank()) { "채팅방 제목은 비어있을 수 없습니다." }
            return ChatRoomTitle(value)
        }
    }

    override fun toString(): String = value
}
