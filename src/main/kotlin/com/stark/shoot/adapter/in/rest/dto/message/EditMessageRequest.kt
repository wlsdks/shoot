package com.stark.shoot.adapter.`in`.rest.dto.message

data class EditMessageRequest(
    val messageId: String,
    val newContent: String
)