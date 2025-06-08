package com.stark.shoot.application.port.`in`.user.block

interface UserBlockUseCase {
    fun blockUser(currentUserId: Long, targetUserId: Long)
    fun unblockUser(currentUserId: Long, targetUserId: Long)
}
