package com.stark.shoot.application.port.`in`.user.group

import com.stark.shoot.application.port.`in`.user.group.command.*
import com.stark.shoot.domain.user.FriendGroup

interface ManageFriendGroupUseCase {
    fun createGroup(command: CreateGroupCommand): FriendGroup
    fun renameGroup(command: RenameGroupCommand): FriendGroup
    fun updateDescription(command: UpdateDescriptionCommand): FriendGroup
    fun addMember(command: AddMemberCommand): FriendGroup
    fun removeMember(command: RemoveMemberCommand): FriendGroup
    fun deleteGroup(command: DeleteGroupCommand)
}
