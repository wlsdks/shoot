package com.stark.shoot.adapter.`in`.web.dto.user

// 친구 응답 DTO 정의 (확장)
data class FriendResponse(
    val id: Long,
    val username: String,
    val nickname: String,
    val profileImageUrl: String?
)