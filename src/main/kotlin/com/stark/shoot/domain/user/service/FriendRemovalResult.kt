package com.stark.shoot.domain.user.service

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.event.FriendRemovedEvent

/**
 * 친구 관계 제거 결과
 */
data class FriendRemovalResult(
    val updatedCurrentUser: User,
    val updatedFriend: User,
    val events: List<FriendRemovedEvent>
)