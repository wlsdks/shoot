package com.stark.shoot.domain.chat.user

@JvmInline
value class RefreshTokenValue private constructor(val value: String) {
    companion object {
        fun from(value: String): RefreshTokenValue {
            require(value.isNotBlank()) { "리프레시 토큰 값은 비어있을 수 없습니다." }
            return RefreshTokenValue(value)
        }
    }

    override fun toString(): String = value
}
