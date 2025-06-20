package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.user.vo.UserId

interface UserDeletePort {
    fun deleteUser(userId: UserId)
}