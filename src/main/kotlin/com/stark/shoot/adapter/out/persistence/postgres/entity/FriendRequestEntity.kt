package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.domain.social.type.FriendRequestStatus
import jakarta.persistence.*
import java.time.Instant

// 친구 요청 정보를 따로 관리하여, 보낸 요청과 받은 요청을 모두 처리할 수 있습니다.
@Entity
@Table(
    name = "friend_requests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_friend_request_sender_receiver_status",
            columnNames = ["sender_id", "receiver_id", "status"]
        )
    ]
)
class FriendRequestEntity(
    // 친구 요청을 보낸 사람
    @Column(name = "sender_id", nullable = false)
    val senderId: Long,

    // 친구 요청을 받은 사람
    @Column(name = "receiver_id", nullable = false)
    val receiverId: Long,

    @Enumerated(EnumType.STRING)
    var status: FriendRequestStatus = FriendRequestStatus.PENDING,

    // 요청 보낸 시간 등 추가 정보도 관리 가능
    val requestDate: Instant = Instant.now(),

    var respondedAt: Instant? = null,
) : BaseEntity()
