package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.apache.kafka.common.errors.DuplicateResourceException
import org.springframework.transaction.annotation.Transactional
import java.util.*

@UseCase
@Transactional
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
        val updatedUser = user.copy(userCode = newCode)
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
        val randomCode = generateRandomUserCode()

        // 유저 코드 업데이트
        val updatedUser = user.copy(userCode = randomCode)
        userUpdatePort.updateUser(updatedUser)
    }

    /**
     * 유저 코드 유효성 검사
     *
     * @param code 검사할 유저 코드
     * @throws InvalidInputException 유효하지 않은 코드인 경우
     */
    private fun validateUserCode(code: String) {
        // 길이 검사 (4~12자)
        if (code.length < 4 || code.length > 12) {
            throw InvalidInputException("유저 코드는 4~12자 사이여야 합니다.")
        }

        // 영문자, 숫자만 허용 (정규식 패턴 검사)
        if (!code.matches(Regex("^[a-zA-Z0-9]+$"))) {
            throw InvalidInputException("유저 코드는 영문자와 숫자만 포함할 수 있습니다.")
        }
    }

    /**
     * 랜덤 유저 코드 생성
     *
     * @return 생성된 랜덤 코드
     */
    private fun generateRandomUserCode(): String {
        // UUID에서 8자리 코드 생성 (영문자와 숫자로 구성)
        return UUID.randomUUID().toString()
            .replace("-", "")
            .substring(0, 8)
            .uppercase()
    }

}