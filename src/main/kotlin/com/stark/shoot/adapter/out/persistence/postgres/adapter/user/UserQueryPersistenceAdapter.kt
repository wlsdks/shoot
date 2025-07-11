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
