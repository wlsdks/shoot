package com.stark.shoot.domain.user.vo

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