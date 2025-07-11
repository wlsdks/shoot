package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.`in`.user.code.command.RemoveUserCodeCommand
import com.stark.shoot.application.port.`in`.user.code.command.UpdateUserCodeCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.apache.kafka.common.errors.DuplicateResourceException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageUserCodeService(
    private val userQueryPort: UserQueryPort,
    private val updateUserCodePort: UpdateUserCodePort,
    private val userCommandPort: UserCommandPort
) : ManageUserCodeUseCase {

    /**
     * 유저 코드 업데이트
     *
     * @param command 유저 코드 업데이트 커맨드
     */
    override fun updateUserCode(command: UpdateUserCodeCommand) {
        // 사용자 존재 여부 확인
        val user = userQueryPort.findUserById(command.userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.userId}")

        // 중복 코드 확인
        val existingUserWithCode = userQueryPort.findByUserCode(command.newCode)
        if (existingUserWithCode != null && existingUserWithCode.id != command.userId) {
            throw DuplicateResourceException("이미 사용 중인 유저 코드입니다: ${command.newCode}")
        }

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = command.newCode)
        updateUserCodePort.updateUserCode(updatedUser)
    }

    /**
     * 유저 코드 삭제 (랜덤 코드로 대체)
     *
     * @param command 유저 코드 삭제 커맨드
     */
    override fun removeUserCode(command: RemoveUserCodeCommand) {
        // 사용자 존재 여부 확인
        val user = userQueryPort.findUserById(command.userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${command.userId}")

        // 새로운 랜덤 코드 생성
        val randomCode = user.generateUserCode()

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = randomCode)
        userCommandPort.updateUser(updatedUser)
    }

}
