package com.stark.shoot.application.port.`in`.user.group

import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.vo.UserId

interface ManageFriendGroupUseCase {
    fun createGroup(ownerId: UserId, name: String, description: String?): FriendGroup
    fun renameGroup(groupId: Long, newName: String): FriendGroup
    fun updateDescription(groupId: Long, description: String?): FriendGroup
    fun addMember(groupId: Long, memberId: UserId): FriendGroup
    fun removeMember(groupId: Long, memberId: UserId): FriendGroup
    fun deleteGroup(groupId: Long)
}
