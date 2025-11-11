package com.stark.shoot.domain.notification.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class NotificationId private constructor(val value: String) {
    companion object {
        fun from(value: String): NotificationId {
            require(value.isNotBlank()) { "알림 ID는 비어있을 수 없습니다." }
            return NotificationId(value)
        }
    }

    override fun toString(): String = value
}
