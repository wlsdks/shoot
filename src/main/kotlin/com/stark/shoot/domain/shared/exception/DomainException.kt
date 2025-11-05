package com.stark.shoot.domain.shared.exception

/**
 * 도메인 규칙 위반 시 발생하는 예외의 기본 클래스
 */
abstract class DomainException(
    override val message: String,
    val errorCode: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)