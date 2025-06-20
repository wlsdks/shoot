package com.stark.shoot.domain.user.vo

@JvmInline
value class UserBio private constructor(val value: String) {
    companion object {
        fun from(value: String): UserBio {
            require(value.length <= 200) { "자기소개는 200자 이하여야 합니다." }
            return UserBio(value)
        }
    }

    override fun toString(): String = value
}
