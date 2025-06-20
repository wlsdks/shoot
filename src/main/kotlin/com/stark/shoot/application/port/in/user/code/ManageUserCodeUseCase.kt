package com.stark.shoot.application.port.`in`.user.code

import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId

interface ManageUserCodeUseCase {
    fun updateUserCode(userId: UserId, newCode: UserCode)
    fun removeUserCode(userId: UserId)
}
