package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.group

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupMemberEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.FriendGroupMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupMemberRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupRepository
import com.stark.shoot.application.port.out.user.group.FriendGroupCommandPort
import com.stark.shoot.domain.social.FriendGroup
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@Adapter
class FriendGroupCommandPersistenceAdapter(
    private val groupRepository: FriendGroupRepository,
    private val memberRepository: FriendGroupMemberRepository,
    private val mapper: FriendGroupMapper,
) : FriendGroupCommandPort {

    override fun save(group: FriendGroup): FriendGroup {
        val entity: FriendGroupEntity = if (group.id != null) {
            val existing = groupRepository.findById(group.id.value)
                .orElseThrow { ResourceNotFoundException("그룹을 찾을 수 없습니다: ${group.id}") }
            existing.name = group.name.value
            existing.description = group.description
            existing
        } else {
            mapper.toEntity(group)
        }

        val saved = groupRepository.save(entity)

        val currentMembers = memberRepository.findAllByGroupId(saved.id)
        val currentIds = currentMembers.map { UserId.from(it.memberId) }.toSet()
        val toAdd = group.memberIds - currentIds
        val toRemove = currentIds - group.memberIds

        toAdd.forEach { memberId ->
            memberRepository.save(FriendGroupMemberEntity(saved.id, memberId.value))
        }

        toRemove.forEach { memberId ->
            memberRepository.deleteByGroupIdAndMemberId(saved.id, memberId.value)
        }

        val members = memberRepository.findAllByGroupId(saved.id).map { it.memberId }.toSet()
        return mapper.toDomain(saved, members)
    }

    override fun deleteById(groupId: Long) {
        memberRepository.deleteAllByGroupId(groupId)
        groupRepository.deleteById(groupId)
    }

}
