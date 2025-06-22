package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import java.time.Instant

@Adapter
class FriendPersistenceAdapter(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipMappingRepository: FriendshipMappingRepository
) : UpdateFriendPort {


    override fun removeOutgoingFriendRequest(
        userId: UserId,
        targetUserId: UserId
    ) {
        // 친구 요청 상태를 취소로 변경 (중복된 요청이 있을 수 있으므로 모두 처리)
        val requests = friendRequestRepository
            .findAllBySenderIdAndReceiverId(userId.value, targetUserId.value)

        for (entity in requests) {
            entity.status = FriendRequestStatus.CANCELLED
            entity.respondedAt = Instant.now()
            friendRequestRepository.save(entity)
        }
    }

    override fun removeIncomingFriendRequest(
        userId: UserId,
        fromUserId: UserId
    ) {
        // 받은 요청을 거절 상태로 변경 (중복된 요청이 있을 수 있으므로 모두 처리)
        val requests = friendRequestRepository
            .findAllBySenderIdAndReceiverId(fromUserId.value, userId.value)

        for (entity in requests) {
            entity.status = FriendRequestStatus.REJECTED
            entity.respondedAt = Instant.now()
            friendRequestRepository.save(entity)
        }
    }

    override fun addFriendRelation(
        userId: UserId,
        friendId: UserId
    ) {
        // 이미 친구 관계가 존재하는지 확인
        if (friendshipMappingRepository.existsByUserIdAndFriendId(userId.value, friendId.value)) {
            return // 이미 친구 관계면 중복 생성하지 않음
        }

        // 애플리케이션 서비스에서 이미 사용자 존재 여부를 확인했으므로 여기서는 존재한다고 가정
        val user = userRepository.getReferenceById(userId.value)
        val friend = userRepository.getReferenceById(friendId.value)

        // 새로운 친구 관계 생성 및 저장
        val friendship = FriendshipMappingEntity(user, friend)
        friendshipMappingRepository.save(friendship)
    }

    override fun removeFriendRelation(
        userId: UserId,
        friendId: UserId
    ) {
        friendshipMappingRepository.deleteByUserIdAndFriendId(userId.value, friendId.value)
    }

}
