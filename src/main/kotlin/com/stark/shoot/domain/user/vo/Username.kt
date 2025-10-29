@file:JvmName("Username")

package com.stark.shoot.domain.user.vo

import com.stark.shoot.domain.exception.InvalidUserDataException

@JvmInline
value class Username private constructor(val value: String) {
    companion object {
        fun from(username: String): Username {
            if (username.isBlank()) {
                throw InvalidUserDataException("사용자명은 비어있을 수 없습니다.")
            }
            if (username.length < 3 || username.length > 20) {
                throw InvalidUserDataException("사용자명은 3-20자 사이여야 합니다.")
            }
            return Username(username)
        }
    }

    override fun toString(): String = value
}
