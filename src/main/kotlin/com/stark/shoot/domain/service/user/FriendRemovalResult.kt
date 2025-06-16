package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.chat.event.FriendRemovedEvent

/**
 * 친구 관계 제거 결과
 */
data class FriendRemovalResult(
    val updatedCurrentUser: User,
    val updatedFriend: User,
    val events: List<FriendRemovedEvent>
)