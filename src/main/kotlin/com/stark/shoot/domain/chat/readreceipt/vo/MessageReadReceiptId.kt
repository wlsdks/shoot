package com.stark.shoot.domain.chat.readreceipt.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 메시지 읽음 표시 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class MessageReadReceiptId(val value: Long) {
    companion object {
        fun from(value: Long): MessageReadReceiptId = MessageReadReceiptId(value)
    }
}
