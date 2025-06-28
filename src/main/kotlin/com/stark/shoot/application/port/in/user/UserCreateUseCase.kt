package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.application.port.`in`.user.command.CreateUserCommand
import com.stark.shoot.domain.user.User

interface UserCreateUseCase {
    /**
     * 사용자 생성
     *
     * @param command 사용자 생성 커맨드
     * @return 생성된 사용자 정보
     */
    fun createUser(command: CreateUserCommand): User
}
