package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class UserCommandPersistenceAdapter(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) : UserCommandPort {

    /**
     * 사용자 생성
     *
     * @param user 사용자 정보
     * @return 생성된 사용자 정보
     */
    override fun createUser(
        user: User
    ): User {
        val userEntity = userMapper.toEntity(user)
        val savedUser = userRepository.save(userEntity)
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
        val userEntity = userMapper.toEntity(user)
        val updatedUser = userRepository.save(userEntity)
        return userMapper.toDomain(updatedUser)
    }

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    override fun deleteUser(
        userId: UserId
    ) {
        val userDocument = userRepository.findById(userId.value)
            .orElseThrow { IllegalArgumentException("User not found") }
        val userDomain = userMapper.toDomain(userDocument)
        val deletedUser = userDomain.copy(isDeleted = true)
        userRepository.save(userMapper.toEntity(deletedUser))
    }

}