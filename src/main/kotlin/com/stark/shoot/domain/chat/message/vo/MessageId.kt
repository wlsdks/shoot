package com.stark.shoot.domain.chat.message.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
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