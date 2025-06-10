package com.stark.shoot.domain.service.user.group

import com.stark.shoot.domain.chat.user.FriendGroup

class FriendGroupDomainService {
    fun create(ownerId: Long, name: String, description: String?): FriendGroup {
        require(name.isNotBlank()) { "그룹 이름은 비어있을 수 없습니다." }
        return FriendGroup(ownerId = ownerId, name = name, description = description)
    }

    fun rename(group: FriendGroup, newName: String): FriendGroup = group.rename(newName)

    fun updateDescription(group: FriendGroup, description: String?): FriendGroup =
        group.updateDescription(description)

    fun addMember(group: FriendGroup, memberId: Long): FriendGroup = group.addMember(memberId)

    fun removeMember(group: FriendGroup, memberId: Long): FriendGroup = group.removeMember(memberId)
}
