package com.stark.shoot.domain.chat.bookmark.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 메시지 북마크 ID Value Object
 * String 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class MessageBookmarkId private constructor(val value: String) {
    companion object {
        fun from(value: String): MessageBookmarkId {
            require(value.isNotBlank()) { "북마크 ID는 비어있을 수 없습니다." }
            return MessageBookmarkId(value)
        }
    }

    override fun toString(): String = value
}
