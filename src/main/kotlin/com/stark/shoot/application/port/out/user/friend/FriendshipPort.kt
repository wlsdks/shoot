package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.UserId

/**
 * 친구 관계 관련 포트
 */
interface FriendshipPort {
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