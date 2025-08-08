package com.stark.shoot.adapter.`in`.rest.dto

import org.springframework.http.HttpStatus

/**
 * API 요청 처리 중 발생하는 모든 예외를 표현하는 단일 예외 클래스
 */
class ApiException(
    override val message: String,
    val errorCode: com.stark.shoot.adapter.`in`.rest.dto.ErrorCode,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    val status: HttpStatus get() = errorCode.status
}