package com.stark.shoot.adapter.`in`.rest.dto.message.forward

data class ForwardMessageToUserRequest(
    val originalMessageId: String,
    val targetUserId: Long,
    val forwardingUserId: Long
) {
}