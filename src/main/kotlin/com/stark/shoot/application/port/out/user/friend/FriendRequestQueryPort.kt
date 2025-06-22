package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 요청 조회 관련 포트
 */
interface FriendRequestQueryPort {
    /**
     * 사용자가 보낸 모든 친구 요청 조회
     *
     * @param senderId 요청을 보낸 사용자 ID
     * @param status 요청 상태 (null인 경우 모든 상태)
     * @return 친구 요청 목록
     */
    fun findAllSentRequests(senderId: UserId, status: FriendRequestStatus? = null): List<FriendRequest>

    /**
     * 사용자가 받은 모든 친구 요청 조회
     *
     * @param receiverId 요청을 받은 사용자 ID
     * @param status 요청 상태 (null인 경우 모든 상태)
     * @return 친구 요청 목록
     */
    fun findAllReceivedRequests(receiverId: UserId, status: FriendRequestStatus? = null): List<FriendRequest>

    /**
     * 특정 친구 요청 조회
     *
     * @param senderId 요청을 보낸 사용자 ID
     * @param receiverId 요청을 받은 사용자 ID
     * @param status 요청 상태 (null인 경우 모든 상태)
     * @return 친구 요청 (없는 경우 null)
     */
    fun findRequest(senderId: UserId, receiverId: UserId, status: FriendRequestStatus? = null): FriendRequest?

    /**
     * 친구 요청 존재 여부 확인
     *
     * @param senderId 요청을 보낸 사용자 ID
     * @param receiverId 요청을 받은 사용자 ID
     * @param status 요청 상태 (null인 경우 모든 상태)
     * @return 존재 여부
     */
    fun existsRequest(senderId: UserId, receiverId: UserId, status: FriendRequestStatus? = null): Boolean
}