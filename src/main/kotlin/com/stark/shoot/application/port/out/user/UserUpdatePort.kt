package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.user.User

interface UserUpdatePort {
    fun updateUser(user: User): User
}