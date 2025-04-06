package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// 친구 요청 정보를 따로 관리하여, 보낸 요청과 받은 요청을 모두 처리할 수 있습니다.
@Entity
@Table(name = "friend_requests")
class FriendRequestEntity(
    // 친구 요청을 보낸 사람
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", referencedColumnName = "id")
    val sender: UserEntity,

    // 친구 요청을 받은 사람
    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_id", referencedColumnName = "id")
    val receiver: UserEntity,

    // 요청 보낸 시간 등 추가 정보도 관리 가능
    val requestDate: Instant = Instant.now()
) : BaseEntity()