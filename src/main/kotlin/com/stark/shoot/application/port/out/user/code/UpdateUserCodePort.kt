package com.stark.shoot.application.port.out.user.code

interface UpdateUserCodePort {
    fun updateUserCode(userId: Long, newCode: String)
    fun clearUserCode(userId: Long)
}
