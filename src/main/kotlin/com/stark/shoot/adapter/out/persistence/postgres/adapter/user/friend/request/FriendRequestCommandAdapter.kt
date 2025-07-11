package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.request

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.FriendRequestId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import java.time.Instant

@Adapter
class FriendRequestCommandAdapter(
    private val friendRequestRepository: FriendRequestRepository,
    private val userRepository: UserRepository
) : FriendRequestCommandPort {

    override fun createRequest(
        friendRequest: FriendRequest
    ): FriendRequest {
        // 애플리케이션 서비스에서 이미 사용자 존재 여부를 확인했으므로 여기서는 존재한다고 가정
        val sender = userRepository.getReferenceById(friendRequest.senderId.value)
        val receiver = userRepository.getReferenceById(friendRequest.receiverId.value)

        // 새로운 친구 요청 생성 및 저장
        val entity = FriendRequestEntity(
            sender = sender,
            receiver = receiver,
            status = friendRequest.status,
            requestDate = friendRequest.createdAt,
            respondedAt = friendRequest.respondedAt
        )
        val savedEntity = friendRequestRepository.save(entity)
        return mapToDomain(savedEntity)
    }

    override fun saveFriendRequest(
        request: FriendRequest
    ) {
        // 애플리케이션 서비스에서 이미 사용자 존재 여부를 확인했으므로 여기서는 존재한다고 가정
        val sender = userRepository.getReferenceById(request.senderId.value)
        val receiver = userRepository.getReferenceById(request.receiverId.value)

        val entity = FriendRequestEntity(sender, receiver, request.status)
        friendRequestRepository.save(entity)
    }

    override fun updateStatus(
        senderId: UserId,
        receiverId: UserId,
        status: FriendRequestStatus
    ) {
        val requests = friendRequestRepository
            .findAllBySenderIdAndReceiverId(senderId.value, receiverId.value)

        // 동일한 상태로 이미 존재하는 요청은 제거하여 중복을 방지한다
        val duplicated = requests.filter { it.status == status }
        if (duplicated.isNotEmpty()) {
            friendRequestRepository.deleteAll(duplicated)
            friendRequestRepository.flush()
        }

        // 남은 요청의 상태를 업데이트한다
        val now = Instant.now()
        for (entity in requests.filter { it.status != status }) {
            entity.status = status
            entity.respondedAt = now
            friendRequestRepository.save(entity)
        }
    }

    private fun mapToDomain(
        entity: FriendRequestEntity
    ): FriendRequest {
        return FriendRequest(
            id = entity.id?.let { FriendRequestId.Companion.from(it) },
            senderId = UserId.Companion.from(entity.sender.id),
            receiverId = UserId.Companion.from(entity.receiver.id),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}