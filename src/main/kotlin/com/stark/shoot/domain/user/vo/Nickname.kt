package com.stark.shoot.domain.user.vo

import com.stark.shoot.domain.exception.InvalidUserDataException

@JvmInline
value class Nickname private constructor(val value: String) {
    companion object {
        fun from(nickname: String): Nickname {
            if (nickname.isBlank()) {
                throw InvalidUserDataException("닉네임은 비어있을 수 없습니다.")
            }
            if (nickname.length < 2 || nickname.length > 30) {
                throw InvalidUserDataException("닉네임은 2-30자 사이여야 합니다.")
            }
            return Nickname(nickname)
        }
    }

    override fun toString(): String = value
}
