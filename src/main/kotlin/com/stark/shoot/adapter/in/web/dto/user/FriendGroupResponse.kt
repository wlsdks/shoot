package com.stark.shoot.adapter.`in`.web.dto.user

import com.stark.shoot.domain.chat.user.FriendGroup
import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class FriendGroupResponse(
    val id: Long,
    val ownerId: Long,
    val name: String,
    val description: String?,
    val memberIds: Set<Long>
)

fun FriendGroup.toResponse() = FriendGroupResponse(
    id = id ?: 0L,
    ownerId = ownerId,
    name = name.value,
    description = description,
    memberIds = memberIds
)
