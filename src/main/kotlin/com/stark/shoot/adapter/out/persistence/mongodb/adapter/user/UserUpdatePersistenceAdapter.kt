package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class UserUpdatePersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : UserUpdatePort {

    /**
     * 사용자 정보 수정
     *
     * @param user 사용자 정보
     * @return 수정된 사용자 정보
     */
    override fun updateUser(
        user: User
    ): User {
        val userDocument = userMapper.toDocument(user)
        val updatedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(updatedUser)
    }

    /**
     * 사용자 ID로 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    override fun findUserById(
        userId: ObjectId
    ): User {
        val userDocument = userMongoRepository.findById(userId)
            .orElse(null)
        return userMapper.toDomain(userDocument)
    }

}