package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import com.stark.shoot.application.port.`in`.user.UserStatusUseCase
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserStatusService(
    private val userUpdatePort: UserUpdatePort
) : UserStatusUseCase {

    /**
     * 유저의 상태를 변경합니다.
     *
     * @param userId 유저 ID
     * @param status 변경할 상태
     * @return 변경된 유저 상태 정보
     */
    override fun updateStatus(
        userId: ObjectId,
        status: UserStatus
    ): User {
        val user = userUpdatePort.findUserById(userId)
        val updatedUser = user.copy(status = status, updatedAt = Instant.now())
        return userUpdatePort.updateUser(updatedUser)
    }

}