package com.stark.shoot.application.port.`in`.user.command

import com.stark.shoot.domain.user.vo.UserCode

data class FindUserByCodeCommand(
    val userCode: UserCode
) {

    companion object {
        fun of(code: String): FindUserByCodeCommand {
            return FindUserByCodeCommand(
                userCode = UserCode.from(code)
            )
        }
    }

}