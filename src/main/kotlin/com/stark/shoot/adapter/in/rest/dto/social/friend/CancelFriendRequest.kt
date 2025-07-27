package com.stark.shoot.adapter.`in`.rest.dto.social.friend

data class CancelFriendRequest(
    val userId: Long,
    val targetUserId: Long
)
