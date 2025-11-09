package com.stark.shoot.domain.user.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 리프레시 토큰 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class RefreshTokenId(val value: Long) {
    companion object {
        fun from(value: Long): RefreshTokenId = RefreshTokenId(value)
    }
}
