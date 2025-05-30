package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.user.User

/**
 * 친구 요청 수락 결과
 */
data class FriendAcceptResult(
    val updatedCurrentUser: User,
    val updatedRequester: User,
    val events: List<FriendAddedEvent>
)