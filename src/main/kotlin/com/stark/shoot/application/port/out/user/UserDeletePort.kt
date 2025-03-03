package com.stark.shoot.application.port.out.user

import org.bson.types.ObjectId

interface UserDeletePort {
    fun deleteUser(userId: ObjectId)
}