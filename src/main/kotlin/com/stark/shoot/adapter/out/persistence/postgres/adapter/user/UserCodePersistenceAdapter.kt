package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class UserCodePersistenceAdapter(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) : UpdateUserCodePort {

    /**
     * 사용자 코드 설정
     *
     * @param userId 사용자 ID
     * @param newCode 새로운 사용자 코드
     * @return Unit (void)
     */
    override fun updateUserCode(
        user: User
    ) {
        val userEntity = userMapper.toEntity(user)
        userRepository.save(userEntity)
    }

}