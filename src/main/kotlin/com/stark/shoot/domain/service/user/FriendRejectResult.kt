package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.user.User

/**
 * 친구 요청 거절 결과
 */
data class FriendRejectResult(
    val updatedCurrentUser: User,
    val updatedRequester: User
)