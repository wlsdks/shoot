package com.stark.shoot.adapter.`in`.rest.dto.social.friend

data class SendFriendRequest(
    val userId: Long,
    val targetUserId: Long
) {
}