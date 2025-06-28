package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.application.port.`in`.user.command.DeleteUserCommand

interface UserDeleteUseCase {
    fun deleteUser(command: DeleteUserCommand)
}
