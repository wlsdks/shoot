package com.stark.shoot.domain.service.user.group

import com.stark.shoot.domain.chat.user.FriendGroup
import com.stark.shoot.domain.chat.user.FriendGroupName
import com.stark.shoot.domain.common.vo.UserId

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
    ): FriendGroup = group.rename(newName)

    fun updateDescription(
        group: FriendGroup,
        description: String?
    ): FriendGroup = group.updateDescription(description)

    fun addMember(
        group: FriendGroup,
        memberId: UserId
    ): FriendGroup = group.addMember(memberId)

    fun removeMember(
        group: FriendGroup,
        memberId: UserId
    ): FriendGroup = group.removeMember(memberId)

}
