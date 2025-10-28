package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Adapter
class UserQueryPersistenceAdapter(
    private val userRepository: UserRepository,
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val userMapper: UserMapper
) : UserQueryPort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 사용자명
     * @return 사용자
     */
    override fun findByUsername(
        username: Username
    ): User? {
        val userEntity = userRepository.findByUsername(username.value)
        return userEntity?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자
     */
    override fun findUserById(
        userId: UserId
    ): User? {
        val userEntity = userRepository.findById(userId.value)
        return if (userEntity.isPresent) {
            userMapper.toDomain(userEntity.get())
        } else null
    }

    /**
     * 모든 사용자 조회
     *
     * @return 사용자 목록
     */
    override fun findAll(): List<User> {
        val userDocs = userRepository.findAll()
        return userDocs.map(userMapper::toDomain)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param userCode 사용자 코드
     * @return 사용자
     */
    override fun findByUserCode(
        userCode: UserCode
    ): User? {
        val userDoc = userRepository.findByUserCode(userCode.value) ?: return null
        return userMapper.toDomain(userDoc)
    }

    /**
     * 사용자 ID 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    override fun existsById(userId: UserId): Boolean {
        return userRepository.existsById(userId.value)
    }

    /**
     * 여러 사용자 ID의 존재 여부를 배치로 확인
     * N+1 쿼리 문제를 방지하기 위한 배치 검증
     *
     * PostgreSQL IN 쿼리를 사용하여 한 번의 쿼리로 모든 사용자 존재 여부를 확인합니다.
     */
    override fun findMissingUserIds(userIds: Set<UserId>): Set<UserId> {
        if (userIds.isEmpty()) return emptySet()

        // 모든 사용자 ID를 Long으로 변환
        val userIdValues = userIds.map { it.value }

        // IN 쿼리로 한 번에 존재하는 사용자 조회
        val existingUserIds = userRepository.findAllById(userIdValues)
            .map { it.id!! }
            .toSet()

        // 존재하지 않는 사용자 ID 필터링
        return userIds.filter { it.value !in existingUserIds }.toSet()
    }

    /**
     * 친구 관계 확인 (양방향 모두 확인)
     */
    override fun checkFriendship(
        userId: UserId,
        friendId: UserId
    ): Boolean {
        // 정방향 친구 관계 확인 (내가 상대방을 친구로 추가한 경우)
        val outgoingFriendship = friendshipMappingRepository
            .existsByUserIdAndFriendId(userId.value, friendId.value)

        // 역방향 친구 관계 확인 (상대방이 나를 친구로 추가한 경우)
        val incomingFriendship = friendshipMappingRepository
            .existsByUserIdAndFriendId(friendId.value, userId.value)

        // 어느 한쪽이라도 친구 관계가 있으면 true 반환
        return outgoingFriendship || incomingFriendship
    }

    /**
     * 보낸 친구 요청 확인
     */
    override fun checkOutgoingFriendRequest(
        userId: UserId,
        targetId: UserId
    ): Boolean {
        return friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
            userId.value,
            targetId.value,
            FriendRequestStatus.PENDING
        )
    }

    /**
     * 받은 친구 요청 확인
     */
    override fun checkIncomingFriendRequest(
        userId: UserId,
        requesterId: UserId
    ): Boolean {
        return friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
            requesterId.value,
            userId.value,
            FriendRequestStatus.PENDING
        )
    }

    /**
     * 여러 사용자 ID로 사용자 목록을 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     */
    override fun findAllByIds(userIds: List<UserId>): List<User> {
        if (userIds.isEmpty()) return emptyList()

        val userIdValues = userIds.map { it.value }
        val userEntities = userRepository.findAllById(userIdValues)
        return userEntities.map(userMapper::toDomain)
    }

    /**
     * 친구 관계 배치 확인
     * 여러 대상 사용자와의 친구 관계를 한 번의 쿼리로 확인
     */
    override fun checkFriendshipBatch(userId: UserId, targetIds: List<UserId>): Set<UserId> {
        if (targetIds.isEmpty()) return emptySet()

        val targetIdValues = targetIds.map { it.value }

        // 정방향 친구 관계 조회 (내가 상대방을 친구로 추가한 경우)
        val outgoingFriendships = friendshipMappingRepository
            .findAllByUserIdAndFriendIdIn(userId.value, targetIdValues)
            .map { UserId.from(it.friend.id!!) }
            .toSet()

        return outgoingFriendships
    }

    /**
     * 발신한 친구 요청 배치 확인
     * 여러 대상 사용자에게 보낸 친구 요청을 한 번의 쿼리로 확인
     */
    override fun checkOutgoingFriendRequestBatch(userId: UserId, targetIds: List<UserId>): Set<UserId> {
        if (targetIds.isEmpty()) return emptySet()

        val targetIdValues = targetIds.map { it.value }

        val friendRequests = friendRequestRepository
            .findAllBySenderIdAndReceiverIdInAndStatus(
                userId.value,
                targetIdValues,
                FriendRequestStatus.PENDING
            )
            .map { UserId.from(it.receiver.id!!) }
            .toSet()

        return friendRequests
    }

    /**
     * 수신한 친구 요청 배치 확인
     * 여러 사용자로부터 받은 친구 요청을 한 번의 쿼리로 확인
     */
    override fun checkIncomingFriendRequestBatch(userId: UserId, requesterIds: List<UserId>): Set<UserId> {
        if (requesterIds.isEmpty()) return emptySet()

        val requesterIdValues = requesterIds.map { it.value }

        val friendRequests = friendRequestRepository
            .findAllBySenderIdInAndReceiverIdAndStatus(
                requesterIdValues,
                userId.value,
                FriendRequestStatus.PENDING
            )
            .map { UserId.from(it.sender.id!!) }
            .toSet()

        return friendRequests
    }

}
