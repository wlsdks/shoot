package com.stark.shoot.application.port.out.user

import org.bson.types.ObjectId

interface UpdateUserProfilePort {
    fun setUserCode(userId: ObjectId, newCode: String)
    fun clearUserCode(userId: ObjectId)
}
