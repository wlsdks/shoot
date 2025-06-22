package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserDeleteService(
    private val userCommandPort: UserCommandPort
) : UserDeleteUseCase {

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    override fun deleteUser(userId: UserId) {
        userCommandPort.deleteUser(userId)
    }

}