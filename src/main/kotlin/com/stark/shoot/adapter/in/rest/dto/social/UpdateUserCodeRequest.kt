package com.stark.shoot.adapter.`in`.rest.dto.social

data class UpdateUserCodeRequest(
    val userId: Long,
    val code: String
) {
}