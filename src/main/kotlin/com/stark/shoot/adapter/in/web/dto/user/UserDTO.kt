package com.stark.shoot.adapter.`in`.web.dto.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserStatus
import com.stark.shoot.domain.chat.user.User
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

data class LoginRequest(val username: String, val password: String)
data class UpdateProfileRequest(val nickname: String?, val profileImageUrl: String?, val bio: String?)
data class FindUserRequest(val query: String) // username 또는 userCode로 검색
data class FriendRequest(val targetUserId: String)

data class UpdateStatusRequest(
    val userId: String, // 추가
    val status: UserStatus
)

data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val nickname: String,
    val status: UserStatus,
    val profileImageUrl: String?,
    val bio: String?,
    val userCode: String,
    val lastSeenAt: Instant?
)

// User -> UserResponse 변환 확장 함수
fun User.toResponse() = UserResponse(
    id = id.toString(),
    username = username,
    nickname = nickname,
    status = status,
    profileImageUrl = profileImageUrl,
    bio = bio,
    userCode = userCode,
    lastSeenAt = lastSeenAt
)
