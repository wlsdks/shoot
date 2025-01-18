package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : UserCreatePort {

    override fun createUser(user: User): User {
        val userDocument = userMapper.toDocument(user)
        val savedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(savedUser)
    }

}