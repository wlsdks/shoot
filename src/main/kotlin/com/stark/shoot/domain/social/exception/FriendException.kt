package com.stark.shoot.domain.social.exception

import com.stark.shoot.domain.shared.exception.DomainException

/**
 * 친구 관련 도메인 예외
 *
 * DDD 개선: UserException에서 Social Context로 이동
 * Social Context의 친구 관계 비즈니스 규칙 위반 시 발생
 */
sealed class FriendException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 자기 자신에게 친구 요청을 보내려고 할 때 발생하는 예외
     */
    class SelfFriendRequestNotAllowed(
        message: String = "자기 자신에게 친구 요청을 보낼 수 없습니다."
    ) : FriendException(message, "SELF_FRIEND_REQUEST_NOT_ALLOWED")

    /**
     * 이미 친구 상태일 때 발생하는 예외
     */
    class AlreadyFriends(
        message: String = "이미 친구 상태입니다."
    ) : FriendException(message, "ALREADY_FRIENDS")

    /**
     * 이미 친구 요청을 보냈을 때 발생하는 예외
     */
    class FriendRequestAlreadySent(
        message: String = "이미 친구 요청을 보냈습니다."
    ) : FriendException(message, "FRIEND_REQUEST_ALREADY_SENT")

    /**
     * 상대방이 이미 친구 요청을 보냈을 때 발생하는 예외
     */
    class FriendRequestAlreadyReceived(
        message: String = "상대방이 이미 친구 요청을 보냈습니다. 수락하거나 거절해주세요."
    ) : FriendException(message, "FRIEND_REQUEST_ALREADY_RECEIVED")
}
