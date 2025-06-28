package com.stark.shoot.application.port.`in`.user.code

import com.stark.shoot.application.port.`in`.user.code.command.RemoveUserCodeCommand
import com.stark.shoot.application.port.`in`.user.code.command.UpdateUserCodeCommand

interface ManageUserCodeUseCase {
    fun updateUserCode(command: UpdateUserCodeCommand)
    fun removeUserCode(command: RemoveUserCodeCommand)
}
