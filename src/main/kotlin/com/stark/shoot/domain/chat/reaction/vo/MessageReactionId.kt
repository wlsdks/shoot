package com.stark.shoot.domain.chat.reaction.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 메시지 리액션 ID Value Object
 * MongoDB ObjectId를 String으로 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class MessageReactionId private constructor(val value: String) {
    companion object {
        fun from(value: String): MessageReactionId {
            require(value.isNotBlank()) { "리액션 ID는 비어있을 수 없습니다." }
            return MessageReactionId(value)
        }
    }

    override fun toString(): String = value
}
