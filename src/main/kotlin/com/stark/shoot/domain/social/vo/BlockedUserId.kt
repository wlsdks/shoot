package com.stark.shoot.domain.social.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 차단 관계 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class BlockedUserId(val value: Long) {
    companion object {
        fun from(value: Long): BlockedUserId = BlockedUserId(value)
    }
}
