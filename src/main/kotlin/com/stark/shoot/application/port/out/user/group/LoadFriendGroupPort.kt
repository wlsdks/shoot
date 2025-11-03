package com.stark.shoot.application.port.out.user.group

import com.stark.shoot.domain.social.FriendGroup
import com.stark.shoot.domain.user.vo.UserId

interface LoadFriendGroupPort {
    fun findById(groupId: Long): FriendGroup?
    fun findByOwnerId(ownerId: UserId): List<FriendGroup>
}
