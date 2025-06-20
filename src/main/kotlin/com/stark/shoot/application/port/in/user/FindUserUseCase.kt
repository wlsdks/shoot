package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.chat.user.UserCode
import com.stark.shoot.domain.chat.user.Username
import com.stark.shoot.domain.common.vo.UserId

interface FindUserUseCase {
    fun findById(userId: UserId): User?
    fun findByUsername(username: Username): User?
    fun findByUserCode(userCode: UserCode): User?
}