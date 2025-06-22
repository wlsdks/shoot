package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 요청을 저장하고 상태를 변경하기 위한 포트
 */
interface FriendRequestPort {
    /** 친구 요청 저장 */
    fun saveFriendRequest(request: FriendRequest)

    /** 상태 업데이트 */
    fun updateStatus(senderId: UserId, receiverId: UserId, status: FriendRequestStatus)
}
