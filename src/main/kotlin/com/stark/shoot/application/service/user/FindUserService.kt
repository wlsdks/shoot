package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.`in`.user.command.FindUserByIdCommand
import com.stark.shoot.application.port.`in`.user.command.FindUserByUsernameCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class FindUserService(
    private val userQueryPort: UserQueryPort,
) : FindUserUseCase {

    /**
     * 사용자 ID로 사용자 조회
     *
     * @param command 사용자 ID 조회 커맨드
     * @return 사용자 정보
     */
    override fun findById(command: FindUserByIdCommand): User? {
        return userQueryPort.findUserById(command.userId)
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * @param command 사용자명 조회 커맨드
     * @return 사용자 정보
     */
    override fun findByUsername(command: FindUserByUsernameCommand): User? {
        return userQueryPort.findByUsername(command.username)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param command 사용자 코드 조회 커맨드
     * @return 사용자 정보
     */
    override fun findByUserCode(command: SendFriendRequestByCodeCommand): User? {
        return userQueryPort.findByUserCode(command.userCode)
    }
}
