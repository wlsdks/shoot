package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.group

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupMemberEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.FriendGroupMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupMemberRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.group.DeleteFriendGroupPort
import com.stark.shoot.application.port.out.user.group.LoadFriendGroupPort
import com.stark.shoot.application.port.out.user.group.SaveFriendGroupPort
import com.stark.shoot.domain.chat.user.FriendGroup
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@Adapter
class FriendGroupPersistenceAdapter(
    private val groupRepository: FriendGroupRepository,
    private val memberRepository: FriendGroupMemberRepository,
    private val userRepository: UserRepository,
    private val mapper: FriendGroupMapper,
) : SaveFriendGroupPort, LoadFriendGroupPort, DeleteFriendGroupPort {

    override fun save(group: FriendGroup): FriendGroup {
        val ownerEntity = userRepository.findById(group.ownerId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${group.ownerId}") }

        val entity: FriendGroupEntity = if (group.id != null) {
            val existing = groupRepository.findById(group.id)
                .orElseThrow { ResourceNotFoundException("그룹을 찾을 수 없습니다: ${group.id}") }
            existing.name = group.name.value
            existing.description = group.description
            existing
        } else {
            mapper.toEntity(group, ownerEntity)
        }

        val saved = groupRepository.save(entity)

        val currentMembers = memberRepository.findAllByGroupId(saved.id)
        val currentIds = currentMembers.map { UserId.from(it.member.id) }.toSet()
        val toAdd = group.memberIds - currentIds
        val toRemove = currentIds - group.memberIds

        toAdd.forEach { memberId ->
            val user = userRepository.findById(memberId.value)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $memberId") }

            memberRepository.save(FriendGroupMemberEntity(saved, user))
        }

        toRemove.forEach { memberId ->
            memberRepository.deleteByGroupIdAndMemberId(saved.id, memberId.value)
        }

        val members = memberRepository.findAllByGroupId(saved.id).map { it.member.id }.toSet()
        return mapper.toDomain(saved, members)
    }

    override fun findById(groupId: Long): FriendGroup? {
        val entity = groupRepository.findById(groupId).orElse(null)
            ?: return null

        val members = memberRepository.findAllByGroupId(entity.id)
            .map { it.member.id }.toSet()

        return mapper.toDomain(entity, members)
    }

    override fun findByOwnerId(ownerId: UserId): List<FriendGroup> {
        val groups = groupRepository.findAllByOwnerId(ownerId.value)

        return groups.map { entity ->
            val members = memberRepository.findAllByGroupId(entity.id)
                .map { it.member.id }.toSet()

            mapper.toDomain(entity, members)
        }
    }

    override fun deleteById(groupId: Long) {
        memberRepository.deleteAllByGroupId(groupId)
        groupRepository.deleteById(groupId)
    }
}
