package com.stark.shoot.application.port.`in`.user.code

interface ManageUserCodeUseCase {
    fun updateUserCode(userId: Long, newCode: String)
    fun removeUserCode(userId: Long)
}
