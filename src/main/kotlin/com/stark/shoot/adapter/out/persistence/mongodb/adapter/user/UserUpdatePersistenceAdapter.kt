package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class UserUpdatePersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : UserUpdatePort {

    override fun updateUser(user: User): User {
        val userDocument = userMapper.toDocument(user)
        val updatedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(updatedUser)
    }

    override fun findUserById(userId: ObjectId): User {
        val userDocument = userMongoRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return userMapper.toDomain(userDocument)
    }

}