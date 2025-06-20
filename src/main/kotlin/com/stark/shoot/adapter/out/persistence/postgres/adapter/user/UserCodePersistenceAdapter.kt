package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class UserCodePersistenceAdapter(
    private val userRepository: UserRepository
) : UpdateUserCodePort {

    /**
     * 사용자 코드 설정
     *
     * @param user 업데이트할 사용자 객체
     * @return Unit (void)
     */
    override fun updateUserCode(
        user: User
    ) {
        // ID로 기존 엔티티를 찾아서 업데이트
        user.id?.let { userId ->
            // 사용자가 존재하는지 확인
            if (!userRepository.existsById(userId)) {
                throw IllegalArgumentException("User not found with ID: $userId")
            }

            // JPQL 쿼리로 userCode 필드만 업데이트
            userRepository.updateUserCode(userId, user.userCode.value)
        } ?: throw IllegalArgumentException("User ID cannot be null for update operation")
    }

}
