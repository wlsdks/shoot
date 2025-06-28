package com.stark.shoot.application.port.`in`.user.block

import com.stark.shoot.application.port.`in`.user.block.command.BlockUserCommand
import com.stark.shoot.application.port.`in`.user.block.command.UnblockUserCommand

interface UserBlockUseCase {
    fun blockUser(command: BlockUserCommand)
    fun unblockUser(command: UnblockUserCommand)
}
