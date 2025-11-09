package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.group

import com.stark.shoot.adapter.out.persistence.postgres.mapper.FriendGroupMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupMemberRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendGroupRepository
import com.stark.shoot.application.port.out.user.group.FriendGroupQueryPort
import com.stark.shoot.domain.social.FriendGroup
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class FriendGroupQueryPersistenceAdapter(
    private val groupRepository: FriendGroupRepository,
    private val memberRepository: FriendGroupMemberRepository,
    private val mapper: FriendGroupMapper,
) : FriendGroupQueryPort {

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

}
