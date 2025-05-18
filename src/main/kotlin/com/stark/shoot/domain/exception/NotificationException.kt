package com.stark.shoot.domain.exception

/**
 * 알림 관련 도메인 규칙 위반 시 발생하는 예외
 */
class NotificationException(
    override val message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(
    message = message,
    errorCode = errorCode,
    cause = cause
)
