package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.user.vo.UserId

interface UserDeleteUseCase {
    fun deleteUser(userId: UserId)
}