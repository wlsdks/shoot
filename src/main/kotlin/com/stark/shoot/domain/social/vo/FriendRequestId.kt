package com.stark.shoot.domain.social.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class FriendRequestId private constructor(val value: Long) {
    companion object {
        fun from(value: Long): FriendRequestId {
            require(value > 0) { "친구 요청 ID는 양수여야 합니다." }
            return FriendRequestId(value)
        }
    }

    override fun toString(): String = value.toString()
}