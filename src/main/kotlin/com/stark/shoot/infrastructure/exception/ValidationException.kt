package com.stark.shoot.infrastructure.exception

/**
 * 검증 관련 도메인 예외
 */
sealed class ValidationException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 그룹 채팅방 최소 참여자 수 미달일 때 발생하는 예외
     */
    class GroupChatMinParticipants(
        minParticipants: Int,
        message: String = "그룹 채팅방은 최소 ${minParticipants}명의 참여자가 필요합니다."
    ) : ValidationException(message, "GROUP_CHAT_MIN_PARTICIPANTS")

    /**
     * 그룹 채팅방 최대 참여자 수 초과일 때 발생하는 예외
     */
    class GroupChatMaxParticipants(
        maxParticipants: Int,
        message: String = "그룹 채팅방은 최대 ${maxParticipants}명까지 참여할 수 있습니다."
    ) : ValidationException(message, "GROUP_CHAT_MAX_PARTICIPANTS")
}