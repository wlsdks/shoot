package com.stark.shoot.domain.social

import com.stark.shoot.domain.shared.event.FriendAddedEvent

/**
 * 친구 요청 수락 결과를 담는 Value Object
 *
 * DDD Rich Model: FriendRequest.accept()가 비즈니스 로직을 직접 처리하고
 * 결과를 FriendshipPair로 반환합니다.
 *
 * @property friendship1 첫 번째 친구 관계 (receiverId → senderId)
 * @property friendship2 두 번째 친구 관계 (senderId → receiverId)
 * @property events 발행할 친구 추가 이벤트 목록 (2개)
 */
data class FriendshipPair(
    val friendship1: Friendship,
    val friendship2: Friendship,
    val events: List<FriendAddedEvent>
) {
    init {
        require(events.size == 2) {
            "FriendshipPair must have exactly 2 events (one for each user)"
        }
    }

    /**
     * 모든 Friendship 목록 반환
     */
    fun getAllFriendships(): List<Friendship> = listOf(friendship1, friendship2)
}
