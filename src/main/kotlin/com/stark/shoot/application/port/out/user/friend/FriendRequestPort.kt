package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 요청 관련 포트
 * 
 * @see FriendRequestQueryPort
 * @see FriendRequestCommandPort
 */
interface FriendRequestPort : FriendRequestQueryPort, FriendRequestCommandPort
