package com.stark.shoot.application.port.`in`.user.group

import com.stark.shoot.application.port.`in`.user.group.command.GetGroupCommand
import com.stark.shoot.application.port.`in`.user.group.command.GetGroupsCommand
import com.stark.shoot.domain.user.FriendGroup

interface FindFriendGroupUseCase {
    fun getGroup(command: GetGroupCommand): FriendGroup?
    fun getGroups(command: GetGroupsCommand): List<FriendGroup>
}
