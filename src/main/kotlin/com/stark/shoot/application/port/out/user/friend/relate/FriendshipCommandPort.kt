package com.stark.shoot.application.port.out.user.friend.relate

import com.stark.shoot.domain.social.Friendship
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

    /**
     * 양방향 친구 관계를 원자적으로 생성
     * A→B와 B→A 관계를 동시에 생성하여 데이터 정합성 보장
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     */
    fun createBidirectionalFriendship(userId1: UserId, userId2: UserId)

    /**
     * 양방향 친구 관계를 원자적으로 삭제
     * A→B와 B→A 관계를 동시에 삭제하여 데이터 정합성 보장
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     */
    fun removeBidirectionalFriendship(userId1: UserId, userId2: UserId)
}