package com.stark.shoot.domain.chat.user

@JvmInline
value class FriendGroupName private constructor(val value: String) {
    companion object {
        fun from(value: String): FriendGroupName {
            require(value.isNotBlank()) { "그룹 이름은 비어있을 수 없습니다." }
            require(value.length <= 50) { "그룹 이름은 50자 이하여야 합니다." }
            return FriendGroupName(value)
        }
    }

    override fun toString(): String = value
}
