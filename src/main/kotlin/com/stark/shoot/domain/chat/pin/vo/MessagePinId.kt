package com.stark.shoot.domain.chat.pin.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 메시지 고정 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class MessagePinId(val value: Long) {
    companion object {
        fun from(value: Long): MessagePinId = MessagePinId(value)
    }
}
