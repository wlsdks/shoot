package com.stark.shoot.application.port.out.user.group

import com.stark.shoot.domain.social.FriendGroup
import com.stark.shoot.domain.shared.UserId

interface LoadFriendGroupPort {
    fun findById(groupId: Long): FriendGroup?
    fun findByOwnerId(ownerId: UserId): List<FriendGroup>
}
