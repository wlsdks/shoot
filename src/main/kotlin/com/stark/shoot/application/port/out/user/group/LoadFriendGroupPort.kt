package com.stark.shoot.application.port.out.user.group

import com.stark.shoot.domain.chat.user.FriendGroup

interface LoadFriendGroupPort {
    fun findById(groupId: Long): FriendGroup?
    fun findByOwnerId(ownerId: Long): List<FriendGroup>
}
