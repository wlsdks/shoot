package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
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
}
