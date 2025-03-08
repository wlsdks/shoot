package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.out.user.UserDeletePort
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId

@UseCase
class UserDeleteService(
    private val userDeletePort: UserDeletePort
) : UserDeleteUseCase {

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    override fun deleteUser(userId: ObjectId) {
        userDeletePort.deleteUser(userId)
    }

}