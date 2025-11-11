package com.stark.shoot.domain.notification.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class NotificationMessage private constructor(val value: String) {
    companion object {
        fun from(value: String): NotificationMessage {
            require(value.isNotBlank()) { "알림 메시지는 비어있을 수 없습니다." }
            return NotificationMessage(value)
        }
    }

    override fun toString(): String = value
}
