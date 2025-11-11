package com.stark.shoot.domain.user.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
@JvmInline
value class ProfileImageUrl private constructor(val value: String) {
    companion object {
        private val URL_REGEX = Regex("^(https?://)?.*")
        fun from(value: String): ProfileImageUrl {
            require(value.isNotBlank()) { "프로필 이미지 URL은 비어있을 수 없습니다." }
            require(URL_REGEX.matches(value)) { "유효하지 않은 URL 형식입니다." }
            return ProfileImageUrl(value)
        }
    }

    override fun toString(): String = value
}
