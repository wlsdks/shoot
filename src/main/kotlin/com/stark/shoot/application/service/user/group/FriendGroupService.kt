package com.stark.shoot.application.service.user.group

import com.stark.shoot.application.port.`in`.user.group.FindFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.ManageFriendGroupUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.group.FriendGroupCommandPort
import com.stark.shoot.application.port.out.user.group.FriendGroupQueryPort
import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.service.group.FriendGroupDomainService
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendGroupService(
    private val findUserPort: FindUserPort,
    private val friendGroupQueryPort: FriendGroupQueryPort,
    private val friendGroupCommandPort: FriendGroupCommandPort,
    private val domainService: FriendGroupDomainService,
) : ManageFriendGroupUseCase, FindFriendGroupUseCase {

    override fun createGroup(
        ownerId: UserId,
        name: String,
        description: String?
    ): FriendGroup {
        if (!findUserPort.existsById(ownerId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $ownerId")
        }
        val group = domainService.create(ownerId, name, description)
        return friendGroupCommandPort.save(group)
    }

    override fun renameGroup(
        groupId: Long,
        newName: String
    ): FriendGroup {
        val group = friendGroupQueryPort.findById(groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: $groupId")
        val updated = domainService.rename(group, newName)
        return friendGroupCommandPort.save(updated)
    }

    override fun updateDescription(
        groupId: Long,
        description: String?
    ): FriendGroup {
        val group = friendGroupQueryPort.findById(groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: $groupId")
        val updated = domainService.updateDescription(group, description)
        return friendGroupCommandPort.save(updated)
    }

    override fun addMember(
        groupId: Long,
        memberId: UserId
    ): FriendGroup {
        if (!findUserPort.existsById(memberId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $memberId")
        }

        val group = friendGroupQueryPort.findById(groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: $groupId")

        val updated = domainService.addMember(group, memberId)

        return friendGroupCommandPort.save(updated)
    }

    override fun removeMember(
        groupId: Long,
        memberId: UserId
    ): FriendGroup {
        val group = friendGroupQueryPort.findById(groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: $groupId")

        val updated = domainService.removeMember(group, memberId)
        return friendGroupCommandPort.save(updated)
    }

    override fun deleteGroup(groupId: Long) {
        friendGroupCommandPort.deleteById(groupId)
    }

    override fun getGroup(groupId: Long): FriendGroup? {
        return friendGroupQueryPort.findById(groupId)
    }

    override fun getGroups(ownerId: UserId): List<FriendGroup> {
        return friendGroupQueryPort.findByOwnerId(ownerId)
    }
}
