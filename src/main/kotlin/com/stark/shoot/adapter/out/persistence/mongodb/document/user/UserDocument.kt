package com.stark.shoot.adapter.out.persistence.mongodb.document.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class UserDocument(
    @Indexed(unique = true)
    val username: String,
    val nickname: String,
    val status: String, // OFFLINE, ONLINE, BUSY, AWAY
    val profileImageUrl: String? = null,
    val lastSeenAt: Instant? = null
) : BaseMongoDocument()