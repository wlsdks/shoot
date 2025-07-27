package com.stark.shoot.adapter.`in`.rest.dto.social.code

data class SendFriendRequestByCodeRequest(
    val userId: Long,
    val targetCode: String
) {
}