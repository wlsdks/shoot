package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.out.user.UserDeletePort
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class UserDeleteService(
    private val userDeletePort: UserDeletePort
) : UserDeleteUseCase {

    override fun deleteUser(userId: ObjectId) {
        userDeletePort.deleteUser(userId)
    }

}