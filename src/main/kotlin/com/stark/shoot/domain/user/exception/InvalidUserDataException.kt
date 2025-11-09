package com.stark.shoot.domain.user.exception

import com.stark.shoot.domain.shared.exception.DomainException

/**
 * 유효하지 않은 사용자 데이터로 인해 발생하는 예외
 */
class InvalidUserDataException(
    override val message: String,
    cause: Throwable? = null
) : DomainException(
    message = message,
    errorCode = "INVALID_USER_DATA",
    cause = cause
)