package com.stark.shoot.adapter.`in`.rest.dto.message

data class DeleteMessageRequest(
    val messageId: String,
    val userId: Long // 웹소켓에서 사용자 식별을 위해 추가
)
