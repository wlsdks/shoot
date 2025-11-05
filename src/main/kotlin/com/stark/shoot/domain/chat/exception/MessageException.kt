package com.stark.shoot.domain.chat.exception

import com.stark.shoot.domain.shared.exception.DomainException

/**
 * 메시지 관련 도메인 예외
 */
sealed class MessageException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 메시지 ID가 없을 때 발생하는 예외
     */
    class MissingId(
        message: String = "메시지 ID가 없습니다."
    ) : MessageException(message, "MESSAGE_ID_MISSING")

    /**
     * 메시지를 수정할 수 없을 때 발생하는 예외
     */
    class NotEditable(
        reason: String? = null,
        message: String = reason ?: "메시지를 수정할 수 없습니다."
    ) : MessageException(message, "MESSAGE_NOT_EDITABLE")

    /**
     * 메시지 내용이 비어있을 때 발생하는 예외
     */
    class EmptyContent(
        message: String = "메시지 내용은 비어있을 수 없습니다."
    ) : MessageException(message, "MESSAGE_EMPTY_CONTENT")
}