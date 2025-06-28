package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.`in`.user.command.DeleteUserCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserDeleteService(
    private val userCommandPort: UserCommandPort
) : UserDeleteUseCase {

    /**
     * 사용자 삭제
     *
     * @param command 사용자 삭제 커맨드
     */
    override fun deleteUser(command: DeleteUserCommand) {
        userCommandPort.deleteUser(command.userId)
    }
}
