package com.stark.shoot.adapter.`in`.rest.dto.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import java.time.Instant

@ApplicationDto
data class UserResponse(
    val id: String,
    val username: String,
    val nickname: String,
    val status: UserStatus,
    val profileImageUrl: String?,
    val backgroundImageUrl: String?,
    val bio: String?,
    val userCode: String,
    val lastSeenAt: Instant?
)

// User -> UserResponse 변환 확장 함수
fun User.toResponse() = UserResponse(
    id = id.toString(),
    username = username.value,
    nickname = nickname.value,
    status = status,
    profileImageUrl = profileImageUrl?.value,
    backgroundImageUrl = backgroundImageUrl?.value,
    bio = bio?.value,
    userCode = userCode.value,
    lastSeenAt = lastSeenAt
)