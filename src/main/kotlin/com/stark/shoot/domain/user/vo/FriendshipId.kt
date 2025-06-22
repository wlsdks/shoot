package com.stark.shoot.domain.user.vo

@JvmInline
value class FriendshipId private constructor(val value: Long) {
    companion object {
        fun from(value: Long): FriendshipId {
            require(value > 0) { "친구 관계 ID는 양수여야 합니다." }
            return FriendshipId(value)
        }
    }

    override fun toString(): String = value.toString()
}