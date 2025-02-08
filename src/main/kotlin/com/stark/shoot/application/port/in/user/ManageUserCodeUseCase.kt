package com.stark.shoot.application.port.`in`.user

import org.bson.types.ObjectId

interface ManageUserCodeUseCase {
    fun updateUserCode(userId: ObjectId, newCode: String)
    fun removeUserCode(userId: ObjectId)
}
