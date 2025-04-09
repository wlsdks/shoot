package com.stark.shoot.adapter.out.persistence.postgres.adapter.user

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.user.User
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
        username: String
    ): User? {
        val userEntity = userRepository.findByUsername(username)
        return userEntity?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자
     */
    override fun findUserById(
        userId: Long
    ): User? {
        val userEntity = userRepository.findById(userId)
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
        userCode: String
    ): User? {
        val userDoc = userRepository.findByUserCode(userCode) ?: return null
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
        excludeUserId: Long,
        limit: Int
    ): List<User> {
        val jpql = "SELECT u FROM UserEntity u WHERE u.id <> :excludeUserId ORDER BY function('RANDOM')"
        val query = entityManager.createQuery(jpql, UserEntity::class.java)
        query.setParameter("excludeUserId", excludeUserId)
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
    override fun existsById(userId: Long): Boolean {
        return userRepository.existsById(userId)
    }

    /**
     * 친구 관계 정보를 포함한 사용자 조회
     */
    override fun findUserWithFriendshipsById(userId: Long): User? {
        val userEntity = userRepository.findById(userId).orElse(null) ?: return null
        val user = userMapper.toDomain(userEntity)

        // 친구 목록 조회
        val friendIds = friendshipMappingRepository.findAllByUserId(userId)
            .map { it.friend.id!! }
            .toSet()

        // 도메인 객체에 친구 ID 설정
        user.friendIds = friendIds

        return user
    }

    /**
     * 친구 요청 정보를 포함한 사용자 조회
     */
    override fun findUserWithFriendRequestsById(userId: Long): User? {
        val userEntity = userRepository.findById(userId).orElse(null) ?: return null
        val user = userMapper.toDomain(userEntity)

        // 받은 친구 요청 조회
        val incomingRequestIds = friendRequestRepository.findAllByReceiverId(userId)
            .map { it.sender.id!! }
            .toSet()

        // 보낸 친구 요청 조회
        val outgoingRequestIds = friendRequestRepository.findAllBySenderId(userId)
            .map { it.receiver.id!! }
            .toSet()

        // 도메인 객체에 요청 ID 설정
        user.incomingFriendRequestIds = incomingRequestIds
        user.outgoingFriendRequestIds = outgoingRequestIds

        return user
    }

    /**
     * 모든 관계 정보를 포함한 사용자 조회
     */
    override fun findUserWithAllRelationshipsById(userId: Long): User? {
        val userEntity = userRepository.findById(userId).orElse(null) ?: return null
        val user = userMapper.toDomain(userEntity)

        // 친구 목록 조회
        val friendIds = friendshipMappingRepository.findAllByUserId(userId)
            .map { it.friend.id!! }
            .toSet()

        // 받은 친구 요청 조회
        val incomingRequestIds = friendRequestRepository.findAllByReceiverId(userId)
            .map { it.sender.id!! }
            .toSet()

        // 보낸 친구 요청 조회
        val outgoingRequestIds = friendRequestRepository.findAllBySenderId(userId)
            .map { it.receiver.id!! }
            .toSet()

        // 도메인 객체에 관계 정보 설정
        user.friendIds = friendIds
        user.incomingFriendRequestIds = incomingRequestIds
        user.outgoingFriendRequestIds = outgoingRequestIds

        return user
    }

    /**
     * 친구 관계 확인
     */
    override fun checkFriendship(
        userId: Long,
        friendId: Long
    ): Boolean {
        return friendshipMappingRepository.existsByUserIdAndFriendId(userId, friendId)
    }

    /**
     * 보낸 친구 요청 확인
     */
    override fun checkOutgoingFriendRequest(
        userId: Long,
        targetId: Long
    ): Boolean {
        return friendRequestRepository.existsBySenderIdAndReceiverId(userId, targetId)
    }

    /**
     * 받은 친구 요청 확인
     */
    override fun checkIncomingFriendRequest(
        userId: Long,
        requesterId: Long
    ): Boolean {
        return friendRequestRepository.existsBySenderIdAndReceiverId(requesterId, userId)
    }

}