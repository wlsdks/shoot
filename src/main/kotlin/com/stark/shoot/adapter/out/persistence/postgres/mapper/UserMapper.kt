package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.*
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            username = user.username.value,
            nickname = user.nickname.value,
            status = user.status,
            userCode = user.userCode.value,
            profileImageUrl = user.profileImageUrl?.value,
            backgroundImageUrl = user.backgroundImageUrl?.value,
            lastSeenAt = user.lastSeenAt,
            bio = user.bio?.value,
            passwordHash = user.passwordHash,
            isDeleted = user.isDeleted
        )
    }

    fun toDomain(userEntity: UserEntity): User {
        return User(
            id = userEntity.id?.let(UserId::from),
            username = Username.from(userEntity.username),
            nickname = Nickname.from(userEntity.nickname),
            status = userEntity.status,
            profileImageUrl = userEntity.profileImageUrl?.let { ProfileImageUrl.from(it) },
            backgroundImageUrl = userEntity.backgroundImageUrl?.let { BackgroundImageUrl.from(it) },
            lastSeenAt = userEntity.lastSeenAt,
            bio = userEntity.bio?.let { UserBio.from(it) },
            passwordHash = userEntity.passwordHash,
            isDeleted = userEntity.isDeleted,
            createdAt = userEntity.createdAt,
            updatedAt = userEntity.updatedAt,
            userCode = UserCode.from(userEntity.userCode)
        )
    }

}
