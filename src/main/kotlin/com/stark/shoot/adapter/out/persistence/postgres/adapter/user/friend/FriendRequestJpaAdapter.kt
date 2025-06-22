package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.FriendRequestPort
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import java.time.Instant

@Adapter
class FriendRequestJpaAdapter(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository
) : FriendRequestPort {

    override fun saveFriendRequest(request: FriendRequest) {
        val sender = userRepository.findById(request.senderId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${'$'}{request.senderId.value}") }
        val receiver = userRepository.findById(request.receiverId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${'$'}{request.receiverId.value}") }
        val entity = FriendRequestEntity(sender, receiver, request.status)
        friendRequestRepository.save(entity)
    }

    override fun updateStatus(senderId: UserId, receiverId: UserId, status: FriendRequestStatus) {
        val requests = friendRequestRepository.findAllBySenderIdAndReceiverId(senderId.value, receiverId.value)
        for (entity in requests) {
            entity.status = status
            entity.respondedAt = Instant.now()
            friendRequestRepository.save(entity)
        }
    }
}
