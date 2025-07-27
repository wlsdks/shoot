package com.stark.shoot.adapter.`in`.rest.dto.social.friend

data class RejectFriendRequest(
    val userId: Long,
    val requesterId: Long
) {
}