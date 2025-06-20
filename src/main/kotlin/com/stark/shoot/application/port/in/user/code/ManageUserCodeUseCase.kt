package com.stark.shoot.application.port.`in`.user.code

import com.stark.shoot.domain.chat.user.UserCode
import com.stark.shoot.domain.common.vo.UserId

interface ManageUserCodeUseCase {
    fun updateUserCode(userId: UserId, newCode: UserCode)
    fun removeUserCode(userId: UserId)
}
