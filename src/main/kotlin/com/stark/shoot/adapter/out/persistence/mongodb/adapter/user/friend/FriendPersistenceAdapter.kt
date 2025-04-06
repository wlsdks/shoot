package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@Adapter
class FriendPersistenceAdapter(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val userMapper: UserMapper
) : UpdateFriendPort {

    /**
     * 친구 요청을 추가합니다.
     * @param userId 요청을 보낸 사용자 ID
     * @param targetUserId 요청을 받은 사용자 ID
     */
    override fun addOutgoingFriendRequest(
        userId: Long,
        targetUserId: Long
    ) {
        val sender = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId") }
        val receiver = userRepository.findById(targetUserId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId") }

        // 이미 요청이 존재하는지 확인
        if (friendRequestRepository.existsBySenderIdAndReceiverId(userId, targetUserId)) {
            return // 이미 요청이 있으면 중복 생성하지 않음
        }

        // 새로운 친구 요청 생성 및 저장
        val request = FriendRequestEntity(sender, receiver)
        friendRequestRepository.save(request)
    }

    override fun removeOutgoingFriendRequest(
        userId: Long,
        targetUserId: Long
    ) {
        // 친구 요청 찾기 및 삭제
        friendRequestRepository.deleteBySenderIdAndReceiverId(userId, targetUserId)
    }

    override fun removeIncomingFriendRequest(
        userId: Long,
        fromUserId: Long
    ) {
        // 이 메서드는 JPA 모델에서 removeOutgoingFriendRequest와 동일 (방향만 반대이므로 파라미터 순서만 바꿔서 호출)
        friendRequestRepository.deleteBySenderIdAndReceiverId(fromUserId, userId)
    }

    override fun addFriendRelation(
        userId: Long,
        friendId: Long
    ) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId") }

        val friend = userRepository.findById(friendId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $friendId") }

        // 이미 친구 관계가 존재하는지 확인
        if (friendshipMappingRepository.existsByUserIdAndFriendId(userId, friendId)) {
            return // 이미 친구 관계면 중복 생성하지 않음
        }

        // 새로운 친구 관계 생성 및 저장
        val friendship = FriendshipMappingEntity(user, friend)
        friendshipMappingRepository.save(friendship)
    }

    override fun updateFriends(
        user: User
    ): User {
        // 1. User 엔티티 조회
        val userEntity = userRepository.findById(user.id!!)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${user.id}") }

        // 2. User 도메인 객체의 기본 필드를 엔티티에 업데이트
        // (필요에 따라 구현 - 현재는 친구 관계만 처리)

        // 3. User 엔티티 저장
        userRepository.save(userEntity)

        // 4. 친구 관계 및 요청 갱신을 위해 도메인 객체를 다시 조회하여 반환
        return loadUserWithRelationships(userEntity.id!!)
    }

    /**
     * 친구 관계를 제거합니다.
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     */
    override fun removeFriendRelation(
        userId: Long,
        friendId: Long
    ) {
        friendshipMappingRepository.deleteByUserIdAndFriendId(userId, friendId)
    }

    /**
     * 사용자 정보와 모든 관계(친구, 요청)를 함께 조회하여 도메인 객체로 변환
     */
    private fun loadUserWithRelationships(
        userId: Long
    ): User {
        // 1. 기본 사용자 정보 조회
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId") }

        // 2. 기본 User 도메인 객체 생성
        val user = userMapper.toDomain(userEntity)

        // 3. 친구 목록 로드 (양방향 관계이므로 User가 시작한 관계만 조회)
        val friendIds = mutableSetOf<Long>()

        // 사용자가 추가한 친구들
        friendshipMappingRepository.findAllByUserId(userId)
            .forEach { friendship -> friendIds.add(friendship.friend.id!!) }

        // 4. 받은 친구 요청 로드
        val incomingRequestIds = mutableSetOf<Long>()
        friendRequestRepository.findAllByReceiverId(userId)
            .forEach { request -> incomingRequestIds.add(request.sender.id!!) }

        // 5. 보낸 친구 요청 로드
        val outgoingRequestIds = mutableSetOf<Long>()
        friendRequestRepository.findAllBySenderId(userId)
            .forEach { request -> outgoingRequestIds.add(request.receiver.id!!) }

        // 6. 관계 정보를 도메인 객체에 설정
        user.friendIds = friendIds
        user.incomingFriendRequestIds = incomingRequestIds
        user.outgoingFriendRequestIds = outgoingRequestIds

        return user
    }

}