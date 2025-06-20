package com.stark.shoot.domain.user.service

import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.user.User

/**
 * 친구 요청 수락 결과
 */
data class FriendAcceptResult(
    val updatedCurrentUser: User,
    val updatedRequester: User,
    val events: List<FriendAddedEvent>
)