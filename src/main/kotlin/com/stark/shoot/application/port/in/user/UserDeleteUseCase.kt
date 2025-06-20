package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.common.vo.UserId

interface UserDeleteUseCase {
    fun deleteUser(userId: UserId)
}