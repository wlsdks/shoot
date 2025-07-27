package com.stark.shoot.adapter.`in`.rest.dto.social

data class SendFriendRequestByCodeRequest(
    val userId: Long,
    val targetCode: String
) {
}