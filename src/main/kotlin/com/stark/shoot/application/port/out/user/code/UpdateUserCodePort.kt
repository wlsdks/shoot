package com.stark.shoot.application.port.out.user.code

import com.stark.shoot.domain.user.User

interface UpdateUserCodePort {
    fun updateUserCode(user: User)
}
