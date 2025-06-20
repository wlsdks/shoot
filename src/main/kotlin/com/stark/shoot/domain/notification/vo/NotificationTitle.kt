package com.stark.shoot.domain.notification.vo

@JvmInline
value class NotificationTitle private constructor(val value: String) {
    companion object {
        fun from(value: String): NotificationTitle {
            require(value.isNotBlank()) { "알림 제목은 비어있을 수 없습니다." }
            return NotificationTitle(value)
        }
    }

    override fun toString(): String = value
}
