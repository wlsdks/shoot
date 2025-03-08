package com.stark.shoot.application.service.user.profile

import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.application.port.`in`.user.profile.UserUpdateProfileUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId
import java.time.Instant

@UseCase
class UserUpdateProfileService(
    private val findUserPort: FindUserPort,
    private val userUpdatePort: UserUpdatePort
) : UserUpdateProfileUseCase {

    /**
     * 프로필 수정
     *
     * @param userId 사용자 ID
     * @param request 프로필 수정 요청
     * @return 수정된 사용자 정보
     */
    override fun updateProfile(
        userId: ObjectId,
        request: UpdateProfileRequest
    ): User {
        val user = findUserPort.findUserById(userId)
            ?: throw IllegalArgumentException("User not found")

        val updatedUser = user.copy(
            nickname = request.nickname ?: user.nickname,
            profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
            bio = request.bio ?: user.bio,
            updatedAt = Instant.now()
        )

        return userUpdatePort.updateUser(updatedUser)
    }

}