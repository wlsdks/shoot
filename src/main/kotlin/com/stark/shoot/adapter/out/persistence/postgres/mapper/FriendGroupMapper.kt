package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.domain.chat.user.FriendGroup
import com.stark.shoot.domain.chat.user.FriendGroupName
import com.stark.shoot.domain.common.vo.UserId
import org.springframework.stereotype.Component

@Component
class FriendGroupMapper {

    fun toDomain(
        entity: FriendGroupEntity,
        memberIds: Set<Long>
    ): FriendGroup {
        return FriendGroup(
            id = entity.id,
            ownerId = UserId.from(entity.owner.id),
            name = FriendGroupName.from(entity.name),
            description = entity.description,
            memberIds = memberIds.map { UserId.from(it) }.toSet(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(
        domain: FriendGroup,
        owner: UserEntity
    ): FriendGroupEntity {
        return FriendGroupEntity(
            owner = owner,
            name = domain.name.value,
            description = domain.description
        )
    }

}
