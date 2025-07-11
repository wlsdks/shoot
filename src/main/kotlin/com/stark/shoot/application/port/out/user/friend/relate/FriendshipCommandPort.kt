package com.stark.shoot.application.port.out.user.friend.relate

import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 관계 생성 및 수정 관련 포트
 */
interface FriendshipCommandPort {
    /**
     * 친구 관계 생성
     *
     * @param friendship 친구 관계
     * @return 저장된 친구 관계
     */
    fun createFriendship(friendship: Friendship): Friendship

    /**
     * 친구 관계 제거
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     */
    fun removeFriendship(userId: UserId, friendId: UserId)
}