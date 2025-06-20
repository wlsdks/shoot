package com.stark.shoot.domain.user.service

import com.stark.shoot.domain.user.User

/**
 * 친구 요청 거절 결과
 */
data class FriendRejectResult(
    val updatedCurrentUser: User,
    val updatedRequester: User
)