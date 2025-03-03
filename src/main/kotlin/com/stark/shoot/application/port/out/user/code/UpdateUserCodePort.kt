package com.stark.shoot.application.port.out.user.code

import org.bson.types.ObjectId

interface UpdateUserCodePort {
    fun setUserCode(userId: ObjectId, newCode: String)
    fun clearUserCode(userId: ObjectId)
}
