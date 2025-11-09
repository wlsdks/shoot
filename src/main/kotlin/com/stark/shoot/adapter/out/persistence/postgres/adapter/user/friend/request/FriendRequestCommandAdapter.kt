package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.request

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import java.time.Instant

@Adapter
class FriendRequestCommandAdapter(
    private val friendRequestRepository: FriendRequestRepository
) : FriendRequestCommandPort {

    override fun createRequest(
        friendRequest: FriendRequest
    ): FriendRequest {
        // 새로운 친구 요청 생성 및 저장
        val entity = FriendRequestEntity(
            senderId = friendRequest.senderId.value,
            receiverId = friendRequest.receiverId.value,
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
        val entity = FriendRequestEntity(
            senderId = request.senderId.value,
            receiverId = request.receiverId.value,
            status = request.status
        )
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

    override fun deleteAllByUserId(userId: UserId) {
        // 보낸 요청과 받은 요청 모두 삭제
        friendRequestRepository.deleteBySenderId(userId.value)
        friendRequestRepository.deleteByReceiverId(userId.value)
    }

    private fun mapToDomain(
        entity: FriendRequestEntity
    ): FriendRequest {
        return FriendRequest(
            id = entity.id?.let { FriendRequestId.Companion.from(it) },
            senderId = UserId.Companion.from(entity.senderId),
            receiverId = UserId.Companion.from(entity.receiverId),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}