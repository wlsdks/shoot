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

        // 도메인 객체에 코드 변경 위임 (중복 검사 포함)
        user.changeUserCode(command.newCode) { newCode ->
            val existingUser = userQueryPort.findByUserCode(newCode)
            existingUser == null || existingUser.id == command.userId
        }

        // 업데이트된 사용자 저장
        updateUserCodePort.updateUserCode(user)
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

        // 도메인 객체에 코드 재생성 위임
        user.generateUserCode()

        // 업데이트된 사용자 저장
        userCommandPort.updateUser(user)
    }

}
