package com.stark.shoot.domain.user.service

import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.UserException

/**
 * 친구 관련 도메인 서비스
 * 친구 요청, 수락, 거절, 제거 등의 도메인 로직을 담당합니다.
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
     * @throws IllegalArgumentException 유효하지 않은 요청인 경우
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
            throw UserException.SelfFriendRequestNotAllowed()
        }

        // 이미 친구인지 확인
        if (isFriend) {
            throw UserException.AlreadyFriends()
        }

        // 이미 친구 요청을 보냈는지 확인
        if (hasOutgoingRequest) {
            throw UserException.FriendRequestAlreadySent()
        }

        // 상대방으로부터 이미 친구 요청을 받은 상태인지 확인
        if (hasIncomingRequest) {
            throw UserException.FriendRequestAlreadyReceived()
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
     * @param friendRequest 수락할 친구 요청
     * @return 처리 결과 (업데이트된 친구 요청, 생성된 친구 관계, 이벤트 목록)
     */
    fun processFriendAccept(
        friendRequest: FriendRequest
    ): FriendAcceptResult {
        // 친구 요청 상태 변경
        friendRequest.accept()

        // 양방향 친구 관계 생성
        val friendship1 = Friendship.create(friendRequest.receiverId, friendRequest.senderId)
        val friendship2 = Friendship.create(friendRequest.senderId, friendRequest.receiverId)

        // 이벤트 생성 (양쪽 사용자에게 친구 추가 알림)
        val events = listOf(
            FriendAddedEvent.create(userId = friendRequest.receiverId, friendId = friendRequest.senderId),
            FriendAddedEvent.create(userId = friendRequest.senderId, friendId = friendRequest.receiverId)
        )

        return FriendAcceptResult(
            updatedRequest = friendRequest,
            friendships = listOf(friendship1, friendship2),
            events = events
        )
    }

}

/**
 * 친구 요청 수락 결과
 */
data class FriendAcceptResult(
    val updatedRequest: FriendRequest,
    val friendships: List<Friendship>,
    val events: List<FriendAddedEvent>
)