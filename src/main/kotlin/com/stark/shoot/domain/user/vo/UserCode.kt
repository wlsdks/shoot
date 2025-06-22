package com.stark.shoot.domain.user.vo

import java.util.*

/**
 * 사용자 코드를 나타내는 값 객체
 */
@JvmInline
value class UserCode private constructor(val value: String) {
    companion object {
        private val CODE_REGEX = Regex("^[A-Z0-9]{4,12}$")

        /**
         * 문자열로부터 UserCode 생성
         */
        fun from(code: String): UserCode {
            require(CODE_REGEX.matches(code)) { "유저 코드는 영문 대문자와 숫자로 4~12자여야 합니다." }
            return UserCode(code)
        }

        /**
         * 랜덤 UserCode 생성 (8자리)
         */
        fun generate(): UserCode {
            val random = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .uppercase()
            return UserCode(random)
        }
    }

    override fun toString(): String = value
}
