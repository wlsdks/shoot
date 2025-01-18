package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class UserMapper {

    /**
     * User -> UserDocument 변환
     *
     * @param domain 도메인 모델
     * @return MongoDB 문서 모델
     */
    fun toDocument(domain: User): UserDocument {
        return UserDocument(
            username = domain.username,
            nickname = domain.nickname,
            status = domain.status.name,
            profileImageUrl = domain.profileImageUrl,
            lastSeenAt = domain.lastSeenAt
        ).apply {
            id = domain.id?.let { ObjectId(it) }
        }
    }

    /**
     * UserDocument -> User 변환
     *
     * @param document MongoDB 문서 모델
     * @return 도메인 모델
     */
    fun toDomain(document: UserDocument): User {
        return User(
            id = document.id?.toHexString(),
            username = document.username,
            nickname = document.nickname,
            status = UserStatus.valueOf(document.status),
            profileImageUrl = document.profileImageUrl,
            lastSeenAt = document.lastSeenAt,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
        )
    }

}