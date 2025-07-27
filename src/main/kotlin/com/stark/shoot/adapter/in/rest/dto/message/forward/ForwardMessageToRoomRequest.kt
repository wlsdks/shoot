package com.stark.shoot.adapter.`in`.rest.dto.message.forward

data class ForwardMessageToRoomRequest(
    val originalMessageId: String,
    val targetRoomId: Long,
    val forwardingUserId: Long
) {
}