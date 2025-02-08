package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class RetrieveUserPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : RetrieveUserPort {

    /**
     * 사용자명으로 사용자 조회
     */
    override fun findByUsername(
        username: String
    ): User? {
        val userDocument = userMongoRepository.findByUsername(username)
        return userDocument?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     */
    override fun findById(
        id: ObjectId
    ): User? {
        val userDocument = userMongoRepository.findById(id)
        return if (userDocument.isPresent) {
            userMapper.toDomain(userDocument.get())
        } else null
    }

    /**
     * 모든 사용자 조회
     */
    override fun findAll(): List<User> {
        val docs = userMongoRepository.findAll()
        return docs.map(userMapper::toDomain)
    }

}