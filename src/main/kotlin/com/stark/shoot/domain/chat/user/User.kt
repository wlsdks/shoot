package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import org.bson.types.ObjectId
import java.time.Instant

data class User(

    val id: ObjectId? = null,
    val username: String,  // 로그인용 고유 사용자명
    val nickname: String,  // 표시 이름
    val status: UserStatus = UserStatus.OFFLINE, // OFFLINE, ONLINE, BUSY, AWAY
    val profileImageUrl: String? = null,    // 프로필 이미지 URL
    val lastSeenAt: Instant? = null,        // 마지막 접속 시간
    val bio: String? = null,                // 한줄 소개 (상태 메시지)
    val passwordHash: String? = null,       // 비밀번호 해시 (로그인 시 필요)
    val isDeleted: Boolean = false,         // 계정 삭제 여부 (소프트 딜리트)
    val createdAt: Instant = Instant.now(), // 생성 시간
    val updatedAt: Instant? = null,         // 업데이트 시간

    // === 친구/소셜 기능 필드 ===
    val friends: Set<ObjectId> = emptySet(),                   // 이미 친구인 사용자들의 ObjectId
    val incomingFriendRequests: Set<ObjectId> = emptySet(),    // 내가 받은(아직 수락/거절 전인) 요청 보낸 사용자 IDs
    val outgoingFriendRequests: Set<ObjectId> = emptySet(),    // 내가 보냈지만 아직 상대방이 수락/거절 안 한 사용자 IDs

    // 유저 찾기 코드 (예: "ABCD1234")
    val userCode: String,

    // ============== 인증 ===============
    val refreshToken: String? = null,
    val refreshTokenExpiration: Instant? = null // 만료 시간 추가

)