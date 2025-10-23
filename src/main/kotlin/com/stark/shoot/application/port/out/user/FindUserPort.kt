package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username

interface FindUserPort {
    fun findByUsername(username: Username): User?
    fun findUserById(userId: UserId): User?
    fun findAll(): List<User>
    fun findByUserCode(userCode: UserCode): User?
    fun existsById(userId: UserId): Boolean

    /**
     * 여러 사용자 ID의 존재 여부를 배치로 확인
     * N+1 쿼리 문제를 방지하기 위한 배치 검증
     *
     * @param userIds 확인할 사용자 ID 목록
     * @return 존재하지 않는 사용자 ID 목록 (모두 존재하면 빈 Set)
     */
    fun findMissingUserIds(userIds: Set<UserId>): Set<UserId>

    // 친구 관계 확인을 위한 단순 조회 메서드들
    fun checkFriendship(userId: UserId, friendId: UserId): Boolean
    fun checkOutgoingFriendRequest(userId: UserId, targetId: UserId): Boolean
    fun checkIncomingFriendRequest(userId: UserId, requesterId: UserId): Boolean
}