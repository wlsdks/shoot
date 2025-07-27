package com.stark.shoot.adapter.`in`.rest.dto.social.code

data class UpdateUserCodeRequest(
    val userId: Long,
    val code: String
) {
}