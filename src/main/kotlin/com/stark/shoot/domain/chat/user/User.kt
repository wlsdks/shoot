package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import java.time.Instant

data class User(
    val id: String? = null,
    val username: String,
    val nickname: String,
    val status: UserStatus = UserStatus.OFFLINE,
    val profileImageUrl: String? = null,
    val lastSeenAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)