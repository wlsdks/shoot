package com.stark.shoot.application.port.`in`.user.group

import com.stark.shoot.domain.chat.user.FriendGroup

interface ManageFriendGroupUseCase {
    fun createGroup(ownerId: Long, name: String, description: String?): FriendGroup
    fun renameGroup(groupId: Long, newName: String): FriendGroup
    fun updateDescription(groupId: Long, description: String?): FriendGroup
    fun addMember(groupId: Long, memberId: Long): FriendGroup
    fun removeMember(groupId: Long, memberId: Long): FriendGroup
    fun deleteGroup(groupId: Long)
}
