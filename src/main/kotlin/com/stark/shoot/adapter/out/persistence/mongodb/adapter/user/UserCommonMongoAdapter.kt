package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UserCreatePort
import com.stark.shoot.application.port.out.user.UserDeletePort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId

@Adapter
class UserCommonMongoAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper
) : UserCreatePort, UserDeletePort, UserUpdatePort {

    /**
     * 사용자 생성
     *
     * @param user 사용자 정보
     * @return 생성된 사용자 정보
     */
    override fun createUser(
        user: User
    ): User {
        val userDocument = userMapper.toDocument(user)
        val savedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(savedUser)
    }

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
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    override fun deleteUser(
        userId: ObjectId
    ) {
        val userDocument = userMongoRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        val userDomain = userMapper.toDomain(userDocument)
        val deletedUser = userDomain.copy(isDeleted = true)
        userMongoRepository.save(userMapper.toDocument(deletedUser))
    }

}