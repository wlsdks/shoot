package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Adapter
class FindUserPersistenceAdapter(
    private val userRepository: UserRepository,
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val userMapper: UserMapper
) : FindUserPort {

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
     * 랜덤 사용자 조회 (JPQL 기반)
     *
     * @param excludeUserId 제외할 사용자 ID (Long 타입)
     * @param limit 조회할 사용자 수
     * @return 도메인 객체 User 목록
     */
    override fun findRandomUsers(
        excludeUserId: UserId,
        limit: Int
    ): List<User> {
        val excludeUserIdValue = excludeUserId.value

        val jpql = "SELECT u FROM UserEntity u WHERE u.id <> :excludeUserIdValue ORDER BY function('RANDOM')"
        val query = entityManager.createQuery(jpql, UserEntity::class.java)

        query.setParameter("excludeUserIdValue", excludeUserIdValue)
        query.maxResults = limit

        val userEntities: List<UserEntity> = query.resultList
        return userEntities.map { userMapper.toDomain(it) }
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
     * 친구 관계 정보를 포함한 사용자 조회
     * 
     * 참고: 이 메서드는 더 이상 User 객체에 친구 ID를 설정하지 않습니다.
     * 대신 FriendshipPort를 사용하여 친구 관계를 조회하세요.
     */
    override fun findUserWithFriendshipsById(userId: UserId): User? {
        val userEntity = userRepository.findById(userId.value)
            .orElse(null) ?: return null

        return userMapper.toDomain(userEntity)
    }

    /**
     * 친구 요청 정보를 포함한 사용자 조회
     * 
     * 참고: 이 메서드는 더 이상 User 객체에 친구 요청 ID를 설정하지 않습니다.
     * 대신 FriendRequestPort를 사용하여 친구 요청을 조회하세요.
     */
    override fun findUserWithFriendRequestsById(userId: UserId): User? {
        val userEntity = userRepository.findById(userId.value)
            .orElse(null) ?: return null

        return userMapper.toDomain(userEntity)
    }

    /**
     * 모든 관계 정보를 포함한 사용자 조회
     * 
     * 참고: 이 메서드는 더 이상 User 객체에 관계 정보를 설정하지 않습니다.
     * 대신 FriendshipPort와 FriendRequestPort를 사용하여 관계 정보를 조회하세요.
     */
    override fun findUserWithAllRelationshipsById(userId: UserId): User? {
        val userEntity = userRepository.findById(userId.value)
            .orElse(null) ?: return null

        return userMapper.toDomain(userEntity)
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

}
