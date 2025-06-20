package com.stark.shoot.domain.chat.user

@JvmInline
value class BackgroundImageUrl private constructor(val value: String) {
    companion object {
        private val URL_REGEX = Regex("^https?://.*")
        fun from(value: String): BackgroundImageUrl {
            require(value.isNotBlank()) { "배경 이미지 URL은 비어있을 수 없습니다." }
            require(URL_REGEX.matches(value)) { "유효하지 않은 URL 형식입니다." }
            return BackgroundImageUrl(value)
        }
    }

    override fun toString(): String = value
}
