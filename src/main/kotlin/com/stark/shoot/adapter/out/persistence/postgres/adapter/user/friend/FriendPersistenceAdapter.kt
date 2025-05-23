package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

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

        // 2. 현재 데이터베이스에 저장된 친구 관계 조회
        val currentFriendships = friendshipMappingRepository.findAllByUserId(user.id)
        val currentFriendIds = currentFriendships.map { it.friend.id!! }.toSet()

        // 3. 도메인 모델의 친구 ID와 비교하여 추가/삭제할 친구 관계 파악
        val friendIdsToAdd = user.friendIds.filter { !currentFriendIds.contains(it) }
        val friendIdsToRemove = currentFriendIds.filter { !user.friendIds.contains(it) }

        // 4. 새로운 친구 관계 추가
        friendIdsToAdd.forEach { friendId ->
            addFriendRelation(user.id, friendId)
        }

        // 5. 제거할 친구 관계 삭제
        friendIdsToRemove.forEach { friendId ->
            removeFriendRelation(user.id, friendId)
        }

        // 6. 현재 데이터베이스에 저장된 받은 친구 요청 조회
        val currentIncomingRequests = friendRequestRepository.findAllByReceiverId(user.id)
        val currentIncomingRequestIds = currentIncomingRequests.map { it.sender.id!! }.toSet()

        // 7. 도메인 모델의 받은 친구 요청 ID와 비교하여 제거할 요청 파악
        val incomingRequestIdsToRemove = currentIncomingRequestIds.filter { !user.incomingFriendRequestIds.contains(it) }

        // 8. 제거할 받은 친구 요청 삭제
        incomingRequestIdsToRemove.forEach { senderId ->
            removeIncomingFriendRequest(user.id, senderId)
        }

        // 9. 현재 데이터베이스에 저장된 보낸 친구 요청 조회
        val currentOutgoingRequests = friendRequestRepository.findAllBySenderId(user.id)
        val currentOutgoingRequestIds = currentOutgoingRequests.map { it.receiver.id!! }.toSet()

        // 10. 도메인 모델의 보낸 친구 요청 ID와 비교하여 제거할 요청 파악
        val outgoingRequestIdsToRemove = currentOutgoingRequestIds.filter { !user.outgoingFriendRequestIds.contains(it) }

        // 11. 제거할 보낸 친구 요청 삭제
        outgoingRequestIdsToRemove.forEach { receiverId ->
            removeOutgoingFriendRequest(user.id, receiverId)
        }

        // 12. User 엔티티 저장
        userRepository.save(userEntity)

        // 13. 친구 관계 및 요청 갱신을 위해 도메인 객체를 다시 조회하여 반환
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

        // 3. 친구 목록 로드 (양방향 관계 모두 조회)
        val outgoingFriendIds = mutableSetOf<Long>()
        val incomingFriendIds = mutableSetOf<Long>()

        // 사용자가 추가한 친구들 (정방향 친구 관계)
        friendshipMappingRepository.findAllByUserId(userId)
            .forEach { friendship -> outgoingFriendIds.add(friendship.friend.id!!) }

        // 사용자를 친구로 추가한 사용자들 (역방향 친구 관계)
        friendshipMappingRepository.findAllByFriendId(userId)
            .forEach { friendship -> incomingFriendIds.add(friendship.user.id!!) }

        // 양방향 친구 관계를 합쳐서 전체 친구 목록 생성
        val allFriendIds = outgoingFriendIds.union(incomingFriendIds)

        // 4. 받은 친구 요청 로드
        val incomingRequestIds = mutableSetOf<Long>()
        friendRequestRepository.findAllByReceiverId(userId)
            .forEach { request -> incomingRequestIds.add(request.sender.id!!) }

        // 5. 보낸 친구 요청 로드
        val outgoingRequestIds = mutableSetOf<Long>()
        friendRequestRepository.findAllBySenderId(userId)
            .forEach { request -> outgoingRequestIds.add(request.receiver.id!!) }

        // 6. 관계 정보를 도메인 객체에 설정
        user.friendIds = allFriendIds
        user.incomingFriendRequestIds = incomingRequestIds
        user.outgoingFriendRequestIds = outgoingRequestIds

        return user
    }

}
