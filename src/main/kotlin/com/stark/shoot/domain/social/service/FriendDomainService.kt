package com.stark.shoot.domain.social.service

import com.stark.shoot.domain.shared.event.FriendAddedEvent
import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.Friendship
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.social.exception.FriendException

/**
 * 친구 관련 도메인 서비스
 * 친구 요청, 수락, 거절, 제거 등의 도메인 로직을 담당합니다.
 *
 * DDD 개선: UserException → FriendException 사용
 */
class FriendDomainService {

    /**
     * 친구 요청 유효성을 검증합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 대상 사용자 ID
     * @param isFriend 이미 친구인지 여부
     * @param hasOutgoingRequest 이미 친구 요청을 보냈는지 여부
     * @param hasIncomingRequest 상대방으로부터 이미 친구 요청을 받았는지 여부
     * @throws FriendException 유효하지 않은 요청인 경우
     */
    fun validateFriendRequest(
        currentUserId: UserId,
        targetUserId: UserId,
        isFriend: Boolean,
        hasOutgoingRequest: Boolean,
        hasIncomingRequest: Boolean
    ) {
        // 자기 자신에게 요청하는 경우 방지
        if (currentUserId == targetUserId) {
            throw FriendException.SelfFriendRequestNotAllowed()
        }

        // 이미 친구인지 확인
        if (isFriend) {
            throw FriendException.AlreadyFriends()
        }

        // 이미 친구 요청을 보냈는지 확인
        if (hasOutgoingRequest) {
            throw FriendException.FriendRequestAlreadySent()
        }

        // 상대방으로부터 이미 친구 요청을 받은 상태인지 확인
        if (hasIncomingRequest) {
            throw FriendException.FriendRequestAlreadyReceived()
        }
    }

    /**
     * 친구 요청 생성
     *
     * @param senderId 요청을 보낸 사용자 ID
     * @param receiverId 요청을 받은 사용자 ID
     * @return 생성된 친구 요청
     */
    fun createFriendRequest(
        senderId: UserId,
        receiverId: UserId
    ): FriendRequest {
        return FriendRequest.create(senderId, receiverId)
    }

    /**
     * 친구 요청 수락 처리를 수행합니다.
     *
     * @Deprecated DDD Rich Model 개선: FriendRequest.accept()가 직접 처리하도록 변경됨
     * @param friendRequest 수락할 친구 요청
     * @return 처리 결과 (업데이트된 친구 요청, 생성된 친구 관계, 이벤트 목록)
     */
    @Deprecated(
        message = "Use FriendRequest.accept() instead. This method will be removed in future versions.",
        replaceWith = ReplaceWith("friendRequest.accept()")
    )
    fun processFriendAccept(
        friendRequest: FriendRequest
    ): FriendAcceptResult {
        // DDD Rich Model: FriendRequest.accept()가 직접 처리
        val friendshipPair = friendRequest.accept()

        return FriendAcceptResult(
            updatedRequest = friendRequest,
            friendships = friendshipPair.getAllFriendships(),
            events = friendshipPair.events
        )
    }

}

/**
 * 친구 요청 수락 결과
 *
 * @Deprecated FriendshipPair로 대체됨
 */
@Deprecated(
    message = "Use FriendshipPair instead",
    replaceWith = ReplaceWith("FriendshipPair", "com.stark.shoot.domain.social.FriendshipPair")
)
data class FriendAcceptResult(
    val updatedRequest: FriendRequest,
    val friendships: List<Friendship>,
    val events: List<FriendAddedEvent>
)