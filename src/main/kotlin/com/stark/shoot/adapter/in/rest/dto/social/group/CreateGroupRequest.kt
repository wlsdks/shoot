package com.stark.shoot.adapter.`in`.rest.dto.social.group

data class CreateGroupRequest(
    val ownerId: Long,
    val name: String,
    val description: String?
)
