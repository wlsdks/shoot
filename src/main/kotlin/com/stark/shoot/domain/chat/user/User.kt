package com.stark.shoot.domain.chat.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserStatus
import java.time.Instant

data class User(
    val id: Long? = null,                    // PostgreSQL에서는 Long 타입을 사용
    var username: String,                    // 로그인용 고유 사용자명
    var nickname: String,                    // 표시 이름
    var status: UserStatus = UserStatus.OFFLINE, // 상태 (OFFLINE, ONLINE, BUSY, 등)
    var profileImageUrl: String? = null,     // 프로필 이미지 URL
    var lastSeenAt: Instant? = null,         // 마지막 접속 시간
    var bio: String? = null,                 // 한줄 소개
    var passwordHash: String? = null,        // 비밀번호 해시
    var isDeleted: Boolean = false,          // 계정 삭제 여부
    var createdAt: Instant = Instant.now(),  // 생성 시간
    var updatedAt: Instant? = null,          // 업데이트 시간

    // 친구/소셜 기능 관련 필드
    var friendIds: Set<Long> = emptySet(),             // 이미 친구인 사용자들의 id 목록
    var incomingFriendRequestIds: Set<Long> = emptySet(),// 받은 친구 요청의 사용자 id 목록
    var outgoingFriendRequestIds: Set<Long> = emptySet(),// 보낸 친구 요청의 사용자 id 목록

    // 유저 찾기 코드
    var userCode: String,                    // 예: "ABCD1234"
) {
    fun changeUserCode(newCode: String) {
        this.userCode = newCode
    }
}
