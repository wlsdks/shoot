package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.shared.UserId

interface UserDeletePort {
    fun deleteUser(userId: UserId)
}