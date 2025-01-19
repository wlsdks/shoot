package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User

interface RetrieveUserPort {
    fun findByUsername(username: String): User?
}