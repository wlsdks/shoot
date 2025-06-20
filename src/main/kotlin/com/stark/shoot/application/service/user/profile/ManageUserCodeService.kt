package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.domain.chat.user.UserCode
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.apache.kafka.common.errors.DuplicateResourceException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageUserCodeService(
    private val findUserPort: FindUserPort,
    private val updateUserCodePort: UpdateUserCodePort,
    private val userUpdatePort: UserUpdatePort
) : ManageUserCodeUseCase {

    /**
     * 유저 코드 업데이트
     *
     * @param userId 사용자 ID
     * @param newCode 새 유저 코드
     */
    override fun updateUserCode(
        userId: Long,
        newCode: String
    ) {
        // 유저 코드 유효성 검사
        validateUserCode(newCode)

        // 사용자 존재 여부 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 중복 코드 확인
        val existingUserWithCode = findUserPort.findByUserCode(newCode)
        if (existingUserWithCode != null && existingUserWithCode.id != userId) {
            throw DuplicateResourceException("이미 사용 중인 유저 코드입니다: $newCode")
        }

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = UserCode.from(newCode))
        updateUserCodePort.updateUserCode(updatedUser)
    }

    /**
     * 유저 코드 삭제 (랜덤 코드로 대체)
     *
     * @param userId 사용자 ID
     */
    override fun removeUserCode(
        userId: Long
    ) {
        // 사용자 존재 여부 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 새로운 랜덤 코드 생성
        val randomCode = UserCode.generate()

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = randomCode)
        userUpdatePort.updateUser(updatedUser)
    }

    /**
     * 유저 코드 유효성 검사
     *
     * @param code 검사할 유저 코드
     * @throws InvalidInputException 유효하지 않은 코드인 경우
     */
    private fun validateUserCode(code: String) {
        try {
            UserCode.from(code)
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(e.message ?: "Invalid code")
        }
    }


}
