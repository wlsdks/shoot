package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.FriendRequestStatus
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import java.time.Instant

@Adapter
class FriendPersistenceAdapter(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipMappingRepository: FriendshipMappingRepository
) : UpdateFriendPort {

    /**
     * 친구 요청을 추가합니다.
     * @param userId 요청을 보낸 사용자 ID
     * @param targetUserId 요청을 받은 사용자 ID
     */
    override fun addOutgoingFriendRequest(
        userId: UserId,
        targetUserId: UserId
    ) {
        val sender = userRepository.findById(userId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${userId.value}") }

        val receiver = userRepository.findById(targetUserId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${targetUserId.value}") }

        // 이미 대기 중인 요청이 존재하는지 확인
        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                userId.value,
                targetUserId.value,
                FriendRequestStatus.PENDING
            )
        ) {
            return // 이미 대기 중인 요청이 있으면 중복 생성하지 않음
        }

        // 취소되거나 거절된 요청이 있는지 확인하고 상태를 업데이트
        val existingRequests = friendRequestRepository
            .findAllBySenderIdAndReceiverId(userId.value, targetUserId.value)

        if (existingRequests.isNotEmpty()) {
            // 기존 요청의 상태를 PENDING으로 변경
            for (entity in existingRequests) {
                entity.status = FriendRequestStatus.PENDING
                entity.respondedAt = null
                friendRequestRepository.save(entity)
            }
            return
        }

        // 새로운 친구 요청 생성 및 저장
        val request = FriendRequestEntity(
            sender = sender,
            receiver = receiver,
            status = FriendRequestStatus.PENDING
        )
        friendRequestRepository.save(request)
    }

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
        val user = userRepository.findById(userId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${userId.value}") }

        val friend = userRepository.findById(friendId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendId.value}") }

        // 이미 친구 관계가 존재하는지 확인
        if (friendshipMappingRepository.existsByUserIdAndFriendId(userId.value, friendId.value)) {
            return // 이미 친구 관계면 중복 생성하지 않음
        }

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
