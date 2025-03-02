package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.application.port.out.user.UserDeletePort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : UserCreatePort, UserDeletePort {

    override fun createUser(user: User): User {
        val userDocument = userMapper.toDocument(user)
        val savedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(savedUser)
    }

    override fun deleteUser(userId: ObjectId) {
        val userDocument = userMongoRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val userDomain = userMapper.toDomain(userDocument)

        val deletedUser = userDomain.copy(isDeleted = true)
        userMongoRepository.save(userMapper.toDocument(deletedUser))
    }

}