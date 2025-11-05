package com.stark.shoot.domain.shared

@JvmInline
value class UserId private constructor(val value: Long) {
    companion object {
        fun from(value: Long): UserId {
            require(value > 0) { "사용자 ID는 양수여야 합니다." }
            return UserId(value)
        }
    }

    override fun toString(): String = value.toString()
}