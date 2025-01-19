package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Component

@Component
class RetrieveUserPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : RetrieveUserPort {

    override fun findByUsername(username: String): User? {
        // username을 이용해 userDocument를 찾음
        val userDocument = userMongoRepository.findByUsername(username)

        // userDocument가 null이면 null 반환, 있으면 User로 변환
        return userDocument?.let { userMapper.toDomain(it) }
    }

}