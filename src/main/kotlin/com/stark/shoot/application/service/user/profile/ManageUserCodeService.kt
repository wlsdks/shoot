package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.code.ManageUserCodeUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.infrastructure.common.exception.web.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ManageUserCodeService(
    private val findUserPort: FindUserPort,
    private val updateUserCodePort: UpdateUserCodePort
) : ManageUserCodeUseCase {

    /**
     * 유저 코드 업데이트
     */
    override fun updateUserCode(
        userId: ObjectId,
        newCode: String
    ) {
        // 1. 사용자 존재 여부 확인
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        // 2. 필요하면 newCode의 유효성 검사(길이, 중복 여부 등)
        if (newCode.isBlank()) {
            throw IllegalArgumentException("newCode cannot be blank")
        }

        // 이미 다른 사용자가 사용 중인 코드인이 중복 체크
        val existingUserWithCode = findUserPort.findByCode(newCode)

        if (existingUserWithCode != null && existingUserWithCode.id != userId) {
            throw IllegalArgumentException("newCode already exists")
        }

        // 3. 사용자 코드 업데이트
        updateUserCodePort.setUserCode(userId, newCode)
    }

    /**
     * 유저 코드 삭제
     */
    override fun removeUserCode(
        userId: ObjectId
    ) {
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        // user.userCode가 null이 아닐 때만 삭제
        if (user.userCode != null) {
            updateUserCodePort.clearUserCode(userId)
        }
    }

}