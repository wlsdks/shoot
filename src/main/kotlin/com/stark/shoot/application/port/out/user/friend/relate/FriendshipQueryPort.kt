package com.stark.shoot.application.port.out.user.friend.relate

import com.stark.shoot.domain.social.Friendship
import com.stark.shoot.domain.shared.UserId

/**
 * 친구 관계 조회 관련 포트
 */
interface FriendshipQueryPort {
    /**
     * 사용자의 모든 친구 관계 조회
     *
     * @param userId 사용자 ID
     * @return 친구 관계 목록
     */
    fun findAllFriendships(userId: UserId): List<Friendship>

    /**
     * 친구 관계 확인
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 여부
     */
    fun isFriend(userId: UserId, friendId: UserId): Boolean

    /**
     * 사용자의 친구 수 조회
     *
     * @param userId 사용자 ID
     * @return 친구 수
     */
    fun countByUserId(userId: UserId): Int
}