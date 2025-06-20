package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username

interface FindUserUseCase {
    fun findById(userId: UserId): User?
    fun findByUsername(username: Username): User?
    fun findByUserCode(userCode: UserCode): User?
}