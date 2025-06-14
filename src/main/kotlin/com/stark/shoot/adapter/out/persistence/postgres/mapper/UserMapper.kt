package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            username = user.username,
            nickname = user.nickname,
            status = user.status,
            userCode = user.userCode,
            profileImageUrl = user.profileImageUrl,
            backgroundImageUrl = user.backgroundImageUrl,
            lastSeenAt = user.lastSeenAt,
            bio = user.bio,
            passwordHash = user.passwordHash,
            isDeleted = user.isDeleted
        )
    }

    fun toDomain(userEntity: UserEntity): User {
        return User(
            id = userEntity.id,
            username = userEntity.username,
            nickname = userEntity.nickname,
            status = userEntity.status,
            profileImageUrl = userEntity.profileImageUrl,
            backgroundImageUrl = userEntity.backgroundImageUrl,
            lastSeenAt = userEntity.lastSeenAt,
            bio = userEntity.bio,
            passwordHash = userEntity.passwordHash,
            isDeleted = userEntity.isDeleted,
            createdAt = userEntity.createdAt,
            updatedAt = userEntity.updatedAt,
            friendIds = emptySet(),
            incomingFriendRequestIds = emptySet(),
            outgoingFriendRequestIds = emptySet(),
            blockedUserIds = emptySet(),
            userCode = userEntity.userCode
        )
    }

}
