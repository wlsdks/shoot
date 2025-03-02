package com.stark.shoot.adapter.out.persistence.mongodb.document.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class UserDocument(

    @Indexed(unique = true)
    val username: String,  // 로그인용 고유 사용자명
    val nickname: String,  // 표시 이름
    val status: String,    // OFFLINE, ONLINE, BUSY, AWAY
    val profileImageUrl: String? = null,  // 프로필 이미지 URL
    val lastSeenAt: Instant? = null,      // 마지막 접속 시간
    val bio: String? = null,              // 한줄 소개 (상태 메시지)
    val passwordHash: String? = null,     // 비밀번호 해시 (로그인 시 필요)
    val isDeleted: Boolean = false,       // 계정 삭제 여부 (소프트 딜리트)

    // ============== 친구 ===============
    val friends: Set<ObjectId> = emptySet(),                // 친구 목록 (이미 친구인 사람 ID)
    val incomingFriendRequests: Set<ObjectId> = emptySet(), // 내가 받은 친구 요청(보낸 사람 ID)
    val outgoingFriendRequests: Set<ObjectId> = emptySet(), // 내가 보낸 친구 요청(받는 사람 ID)

    // 유저 찾기 코드 (예: "ABCD1234", 중복은 허용하지 않음)
    @Indexed(unique = true)
    val userCode: String,

    // ============== 인증 ===============
    val refreshToken: String? = null,
    val refreshTokenExpiration: Instant? = null // 만료 시간 추가

) : BaseMongoDocument()