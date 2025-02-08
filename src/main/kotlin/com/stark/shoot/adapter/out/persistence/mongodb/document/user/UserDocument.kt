package com.stark.shoot.adapter.out.persistence.mongodb.document.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import org.bson.types.ObjectId
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
    val lastSeenAt: Instant? = null,

    // ============== 친구 ===============
    val friends: Set<ObjectId> = emptySet(),                // 친구 목록 (이미 친구인 사람 ID)
    val incomingFriendRequests: Set<ObjectId> = emptySet(), // 내가 받은 친구 요청(보낸 사람 ID)
    val outgoingFriendRequests: Set<ObjectId> = emptySet(), // 내가 보낸 친구 요청(받는 사람 ID)

    // 유저 찾기 코드 (예: "ABCD1234", 중복은 허용하지 않음)
    @Indexed(unique = true)
    val userCode: String,

) : BaseMongoDocument()