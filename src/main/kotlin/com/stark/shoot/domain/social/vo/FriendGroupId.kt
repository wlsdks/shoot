package com.stark.shoot.domain.social.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 친구 그룹 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class FriendGroupId(val value: Long) {
    companion object {
        fun from(value: Long): FriendGroupId = FriendGroupId(value)
    }
}
