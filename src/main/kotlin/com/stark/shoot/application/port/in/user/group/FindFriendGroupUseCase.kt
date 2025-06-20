package com.stark.shoot.application.port.`in`.user.group

import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.vo.UserId

interface FindFriendGroupUseCase {
    fun getGroup(groupId: Long): FriendGroup?
    fun getGroups(ownerId: UserId): List<FriendGroup>
}
