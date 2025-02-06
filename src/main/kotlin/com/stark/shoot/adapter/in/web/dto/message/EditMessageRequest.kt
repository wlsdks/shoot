package com.stark.shoot.adapter.`in`.web.dto.message

data class EditMessageRequest(
    val messageId: String,
    val newContent: String
)