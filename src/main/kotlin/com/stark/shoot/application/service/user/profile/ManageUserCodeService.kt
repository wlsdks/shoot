package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.apache.kafka.common.errors.DuplicateResourceException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageUserCodeService(
    private val findUserPort: FindUserPort,
    private val updateUserCodePort: UpdateUserCodePort,
    private val userCommandPort: UserCommandPort
) : ManageUserCodeUseCase {

    /**
     * 유저 코드 업데이트
     *
     * @param userId 사용자 ID
     * @param newCode 새 유저 코드
     */
    override fun updateUserCode(
        userId: UserId,
        newCode: UserCode
    ) {
        // 사용자 존재 여부 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 중복 코드 확인
        val existingUserWithCode = findUserPort.findByUserCode(newCode)
        if (existingUserWithCode != null && existingUserWithCode.id != userId) {
            throw DuplicateResourceException("이미 사용 중인 유저 코드입니다: $newCode")
        }

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = newCode)
        updateUserCodePort.updateUserCode(updatedUser)
    }

    /**
     * 유저 코드 삭제 (랜덤 코드로 대체)
     *
     * @param userId 사용자 ID
     */
    override fun removeUserCode(
        userId: UserId
    ) {
        // 사용자 존재 여부 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        // 새로운 랜덤 코드 생성
        val randomCode = user.generateUserCode()

        // 유저 코드 업데이트
        val updatedUser = user.copy(id = user.id, userCode = randomCode)
        userCommandPort.updateUser(updatedUser)
    }

}
