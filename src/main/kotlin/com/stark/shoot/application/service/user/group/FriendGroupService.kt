package com.stark.shoot.application.service.user.group

import com.stark.shoot.application.port.`in`.user.group.FindFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.ManageFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.command.*
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.group.FriendGroupCommandPort
import com.stark.shoot.application.port.out.user.group.FriendGroupQueryPort
import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.service.group.FriendGroupDomainService
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

    override fun createGroup(command: CreateGroupCommand): FriendGroup {
        if (!findUserPort.existsById(command.ownerId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.ownerId}")
        }
        val group = domainService.create(command.ownerId, command.name, command.description)
        return friendGroupCommandPort.save(group)
    }

    override fun renameGroup(command: RenameGroupCommand): FriendGroup {
        val group = friendGroupQueryPort.findById(command.groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: ${command.groupId}")
        val updated = domainService.rename(group, command.newName)
        return friendGroupCommandPort.save(updated)
    }

    override fun updateDescription(command: UpdateDescriptionCommand): FriendGroup {
        val group = friendGroupQueryPort.findById(command.groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: ${command.groupId}")
        val updated = domainService.updateDescription(group, command.description)
        return friendGroupCommandPort.save(updated)
    }

    override fun addMember(command: AddMemberCommand): FriendGroup {
        if (!findUserPort.existsById(command.memberId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.memberId}")
        }

        val group = friendGroupQueryPort.findById(command.groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: ${command.groupId}")

        val updated = domainService.addMember(group, command.memberId)

        return friendGroupCommandPort.save(updated)
    }

    override fun removeMember(command: RemoveMemberCommand): FriendGroup {
        val group = friendGroupQueryPort.findById(command.groupId)
            ?: throw ResourceNotFoundException("그룹을 찾을 수 없습니다: ${command.groupId}")

        val updated = domainService.removeMember(group, command.memberId)
        return friendGroupCommandPort.save(updated)
    }

    override fun deleteGroup(command: DeleteGroupCommand) {
        friendGroupCommandPort.deleteById(command.groupId)
    }

    override fun getGroup(command: GetGroupCommand): FriendGroup? {
        return friendGroupQueryPort.findById(command.groupId)
    }

    override fun getGroups(command: GetGroupsCommand): List<FriendGroup> {
        return friendGroupQueryPort.findByOwnerId(command.ownerId)
    }
}
