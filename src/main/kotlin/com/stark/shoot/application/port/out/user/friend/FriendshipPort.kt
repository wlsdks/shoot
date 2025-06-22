package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 관계 관련 포트
 * 
 * @see FriendshipQueryPort
 * @see FriendshipCommandPort
 */
interface FriendshipPort : FriendshipQueryPort, FriendshipCommandPort
