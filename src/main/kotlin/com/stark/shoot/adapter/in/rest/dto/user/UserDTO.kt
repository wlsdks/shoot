package com.stark.shoot.adapter.`in`.rest.dto.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

data class CreateUserRequest(
    val username: String,
    val nickname: String,
    val password: String,
    val email: String,
    val bio: String? = null,
    @field:Parameter(content = [Content(mediaType = "multipart/form-data")])
    val profileImage: MultipartFile? = null
)

data class UpdateProfileRequest(
    val nickname: String?,
    val profileImageUrl: String?,
    val backgroundImageUrl: String?,
    val bio: String?
)

data class SetProfileImageRequest(val profileImageUrl: String)
data class SetBackgroundImageRequest(val backgroundImageUrl: String)

data class UpdateStatusRequest(
    val userId: String, // 추가
    val status: UserStatus
)


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
