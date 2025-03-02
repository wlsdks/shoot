package com.stark.shoot.application.service.user.profile

import com.stark.shoot.application.port.`in`.user.UserUpdateProfileUseCase
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserUpdateProfileService(
    private val userUpdatePort: UserUpdatePort
) : UserUpdateProfileUseCase {

    override fun updateProfile(
        userId: ObjectId,
        nickname: String?,
        profileImageUrl: String?,
        bio: String?
    ): User {
        val user = userUpdatePort.findUserById(userId)
        val updatedUser = user.copy(
            nickname = nickname ?: user.nickname,
            profileImageUrl = profileImageUrl ?: user.profileImageUrl,
            bio = bio ?: user.bio,
            updatedAt = Instant.now()
        )
        return userUpdatePort.updateUser(updatedUser)
    }

}