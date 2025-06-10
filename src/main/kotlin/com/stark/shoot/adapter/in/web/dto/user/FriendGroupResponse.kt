package com.stark.shoot.adapter.`in`.web.dto.user

import com.stark.shoot.domain.chat.user.FriendGroup

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
    name = name,
    description = description,
    memberIds = memberIds
)
