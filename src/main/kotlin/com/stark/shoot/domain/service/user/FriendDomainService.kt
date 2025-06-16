package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.event.FriendRemovedEvent
import com.stark.shoot.domain.chat.user.User

/**
 * 친구 관련 도메인 서비스
 * 친구 요청, 수락, 거절 등의 도메인 로직을 담당합니다.
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
        currentUserId: Long,
        targetUserId: Long,
        isFriend: Boolean,
        hasOutgoingRequest: Boolean,
        hasIncomingRequest: Boolean
    ) {
        // 자기 자신에게 요청하는 경우 방지
        if (currentUserId == targetUserId) {
            throw IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.")
        }

        // 이미 친구인지 확인
        if (isFriend) {
            throw IllegalArgumentException("이미 친구 상태입니다.")
        }

        // 이미 친구 요청을 보냈는지 확인
        if (hasOutgoingRequest) {
            throw IllegalArgumentException("이미 친구 요청을 보냈습니다.")
        }

        // 상대방으로부터 이미 친구 요청을 받은 상태인지 확인
        if (hasIncomingRequest) {
            throw IllegalArgumentException("상대방이 이미 친구 요청을 보냈습니다. 수락하거나 거절해주세요.")
        }
    }

    /**
     * 친구 요청 수락 유효성을 검증합니다.
     *
     * @param currentUser 현재 사용자
     * @param requesterId 요청자 ID
     * @throws IllegalArgumentException 유효하지 않은 요청인 경우
     */
    fun validateFriendAccept(
        currentUser: User,
        requesterId: Long
    ) {
        // 친구 요청 존재 여부 확인
        if (!currentUser.incomingFriendRequestIds.contains(requesterId)) {
            throw IllegalArgumentException("해당 친구 요청이 존재하지 않습니다.")
        }
    }

    /**
     * 친구 요청 수락 처리를 수행합니다.
     *
     * @param currentUser 현재 사용자
     * @param requester 요청자
     * @param requesterId 요청자 ID
     * @return 처리 결과 (업데이트된 사용자, 업데이트된 요청자, 이벤트 목록)
     */
    fun processFriendAccept(
        currentUser: User,
        requester: User,
        requesterId: Long
    ): FriendAcceptResult {
        // 도메인 객체의 메서드를 사용하여 친구 요청 수락
        val updatedCurrentUser = currentUser.acceptFriendRequest(requesterId)

        // 요청자의 친구 목록에도 추가
        val updatedRequester = requester.addFriend(currentUser.id!!)

        // 이벤트 생성 (양쪽 사용자에게 친구 추가 알림)
        val events = listOf(
            FriendAddedEvent.create(userId = currentUser.id!!, friendId = requesterId),
            FriendAddedEvent.create(userId = requesterId, friendId = currentUser.id!!)
        )

        return FriendAcceptResult(
            updatedCurrentUser = updatedCurrentUser,
            updatedRequester = updatedRequester,
            events = events
        )
    }

    /**
     * 친구 요청 거절 처리를 수행합니다.
     *
     * @param currentUser 현재 사용자
     * @param requester 요청자
     * @param requesterId 요청자 ID
     * @return 처리 결과 (업데이트된 사용자, 업데이트된 요청자)
     */
    fun processFriendReject(
        currentUser: User,
        requester: User,
        requesterId: Long
    ): FriendRejectResult {
        // 도메인 객체의 메서드를 사용하여 친구 요청 거절
        val updatedCurrentUser = currentUser.rejectFriendRequest(requesterId)

        // 요청자의 발신 요청 목록에서도 제거
        val updatedRequester = requester.cancelFriendRequest(currentUser.id!!)

        return FriendRejectResult(
            updatedCurrentUser = updatedCurrentUser,
            updatedRequester = updatedRequester
        )
    }

    /**
     * 친구 관계 제거 처리를 수행합니다.
     *
     * @param currentUser 현재 사용자
     * @param friend 친구 사용자
     * @param friendId 친구 ID
     * @return 처리 결과 (업데이트된 사용자, 업데이트된 친구)
     */
    fun processFriendRemoval(
        currentUser: User,
        friend: User,
        friendId: Long
    ): FriendRemovalResult {
        // 도메인 객체의 메서드를 사용하여 친구 관계 제거
        val updatedCurrentUser = currentUser.removeFriend(friendId)

        // 친구의 친구 목록에서도 현재 사용자 제거
        val updatedFriend = friend.removeFriend(currentUser.id!!)

        // 이벤트 생성 (양쪽 사용자에게 친구 제거 알림)
        val events = listOf(
            FriendRemovedEvent.create(userId = currentUser.id!!, friendId = friendId),
            FriendRemovedEvent.create(userId = friendId, friendId = currentUser.id!!)
        )

        return FriendRemovalResult(
            updatedCurrentUser = updatedCurrentUser,
            updatedFriend = updatedFriend,
            events = events
        )
    }

}
