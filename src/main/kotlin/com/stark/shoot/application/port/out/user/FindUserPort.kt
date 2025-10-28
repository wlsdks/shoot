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

    /**
     * 여러 사용자 ID로 사용자 목록을 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 사용자 목록 (존재하지 않는 ID는 제외)
     */
    fun findAllByIds(userIds: List<UserId>): List<User>

    /**
     * 친구 관계 배치 확인
     * 여러 대상 사용자와의 친구 관계를 한 번의 쿼리로 확인
     *
     * @param userId 기준 사용자 ID
     * @param targetIds 확인할 대상 사용자 ID 목록
     * @return 친구 관계가 존재하는 사용자 ID 집합
     */
    fun checkFriendshipBatch(userId: UserId, targetIds: List<UserId>): Set<UserId>

    /**
     * 발신한 친구 요청 배치 확인
     * 여러 대상 사용자에게 보낸 친구 요청을 한 번의 쿼리로 확인
     *
     * @param userId 기준 사용자 ID (발신자)
     * @param targetIds 확인할 대상 사용자 ID 목록 (수신자)
     * @return 친구 요청을 보낸 사용자 ID 집합
     */
    fun checkOutgoingFriendRequestBatch(userId: UserId, targetIds: List<UserId>): Set<UserId>

    /**
     * 수신한 친구 요청 배치 확인
     * 여러 사용자로부터 받은 친구 요청을 한 번의 쿼리로 확인
     *
     * @param userId 기준 사용자 ID (수신자)
     * @param requesterIds 확인할 요청자 ID 목록 (발신자)
     * @return 친구 요청을 보낸 사용자 ID 집합
     */
    fun checkIncomingFriendRequestBatch(userId: UserId, requesterIds: List<UserId>): Set<UserId>
}