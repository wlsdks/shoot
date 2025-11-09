package com.stark.shoot.domain.shared.exception

/**
 * 사용자 관련 도메인 예외
 *
 * DDD 개선: Context별 예외 분리
 * - Social Context 예외 → FriendException으로 이동
 * - ChatRoom Context 예외 → ParticipantException으로 이동
 * - Shared: 여러 Context에서 공통 사용되는 사용자 예외만 유지
 */
sealed class UserException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 존재하지 않는 사용자일 때 발생하는 예외
     */
    class NotFound(
        userId: Long,
        message: String = "존재하지 않는 사용자입니다: $userId"
    ) : UserException(message, "USER_NOT_FOUND")

    /**
     * 인증 정보가 올바르지 않을 때 발생하는 예외 (비밀번호 불일치 등)
     */
    class InvalidCredentials(
        message: String = "인증 정보가 올바르지 않습니다."
    ) : UserException(message, "INVALID_CREDENTIALS")
}