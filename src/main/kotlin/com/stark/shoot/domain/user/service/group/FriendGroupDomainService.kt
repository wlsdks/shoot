package com.stark.shoot.domain.user.service.group

import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.vo.FriendGroupName
import com.stark.shoot.domain.user.vo.UserId

class FriendGroupDomainService {

    fun create(
        ownerId: UserId,
        name: String,
        description: String?
    ): FriendGroup {
        val nameVo = FriendGroupName.from(name)
        return FriendGroup(ownerId = ownerId, name = nameVo, description = description)
    }

    fun rename(
        group: FriendGroup,
        newName: String
    ): FriendGroup {
        group.rename(newName)
        return group
    }

    fun updateDescription(
        group: FriendGroup,
        description: String?
    ): FriendGroup {
        group.updateDescription(description)
        return group
    }

    fun addMember(
        group: FriendGroup,
        memberId: UserId
    ): Boolean = group.addMember(memberId)

    fun removeMember(
        group: FriendGroup,
        memberId: UserId
    ): Boolean = group.removeMember(memberId)

}
