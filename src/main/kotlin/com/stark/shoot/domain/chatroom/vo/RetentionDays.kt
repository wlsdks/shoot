package com.stark.shoot.domain.chatroom.vo

@JvmInline
value class RetentionDays private constructor(val value: Int) {
    companion object {
        fun from(value: Int): RetentionDays {
            require(value > 0) { "보존 기간은 1일 이상이어야 합니다." }
            return RetentionDays(value)
        }
    }

    override fun toString(): String = value.toString()
}
