package com.stark.shoot.application.port.`in`.user.block

import com.stark.shoot.domain.common.vo.UserId

interface UserBlockUseCase {
    fun blockUser(currentUserId: UserId, targetUserId: UserId)
    fun unblockUser(currentUserId: UserId, targetUserId: UserId)
}
