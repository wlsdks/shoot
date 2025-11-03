package com.stark.shoot.application.port.out.user.friend.request

import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 요청 생성 및 수정 관련 포트
 */
interface FriendRequestCommandPort {
    /**
     * 친구 요청 생성
     *
     * @param friendRequest 친구 요청
     * @return 저장된 친구 요청
     */
    fun createRequest(friendRequest: FriendRequest): FriendRequest

    /** 
     * 친구 요청 저장
     * 
     * @param request 저장할 친구 요청
     */
    fun saveFriendRequest(request: FriendRequest)

    /**
     * 상태 업데이트
     *
     * @param senderId 요청을 보낸 사용자 ID
     * @param receiverId 요청을 받은 사용자 ID
     * @param status 업데이트할 상태
     */
    fun updateStatus(senderId: UserId, receiverId: UserId, status: FriendRequestStatus)

    /**
     * 사용자의 모든 친구 요청을 삭제합니다.
     * 보낸 요청과 받은 요청 모두 삭제됩니다.
     *
     * @param userId 삭제할 사용자 ID
     */
    fun deleteAllByUserId(userId: UserId)
}
