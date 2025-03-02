package com.stark.shoot.application.port.`in`.user

import org.bson.types.ObjectId

interface UserDeleteUseCase {
    fun deleteUser(userId: ObjectId)
}