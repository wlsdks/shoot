package com.stark.shoot.application.port.out.user.group

import com.stark.shoot.domain.chat.user.FriendGroup
import com.stark.shoot.domain.common.vo.UserId

interface LoadFriendGroupPort {
    fun findById(groupId: Long): FriendGroup?
    fun findByOwnerId(ownerId: UserId): List<FriendGroup>
}
