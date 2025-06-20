package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.user.User

interface UserCreatePort {
    fun createUser(user: User): User
}