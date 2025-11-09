package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.request

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class FriendRequestQueryAdapter(
    private val friendRequestRepository: FriendRequestRepository,
) : FriendRequestQueryPort {

    override fun findAllSentRequests(
        senderId: UserId,
        status: FriendRequestStatus?
    ): List<FriendRequest> {
        return if (status != null) {
            friendRequestRepository.findAllBySenderIdAndStatus(senderId.value, status)
        } else {
            // 모든 상태의 요청을 조회하기 위해 각 상태별로 조회 후 합침
            FriendRequestStatus.values().flatMap {
                friendRequestRepository.findAllBySenderIdAndStatus(senderId.value, it)
            }
        }.map { mapToDomain(it) }
    }

    override fun findAllReceivedRequests(
        receiverId: UserId,
        status: FriendRequestStatus?
    ): List<FriendRequest> {
        return if (status != null) {
            friendRequestRepository.findAllByReceiverIdAndStatus(receiverId.value, status)
        } else {
            // 모든 상태의 요청을 조회하기 위해 각 상태별로 조회 후 합침
            FriendRequestStatus.values().flatMap {
                friendRequestRepository.findAllByReceiverIdAndStatus(receiverId.value, it)
            }
        }.map { mapToDomain(it) }
    }

    override fun findRequest(
        senderId: UserId,
        receiverId: UserId,
        status: FriendRequestStatus?
    ): FriendRequest? {
        return if (status != null) {
            // 특정 상태의 요청만 조회
            friendRequestRepository.findAllBySenderIdAndReceiverId(senderId.value, receiverId.value)
                .firstOrNull { it.status == status }
        } else {
            // 상태 무관하게 조회
            friendRequestRepository.findBySenderIdAndReceiverId(senderId.value, receiverId.value)
        }?.let { mapToDomain(it) }
    }

    override fun existsRequest(
        senderId: UserId,
        receiverId: UserId,
        status: FriendRequestStatus?
    ): Boolean {
        return if (status != null) {
            friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId.value, receiverId.value, status)
        } else {
            // 상태 무관하게 존재 여부 확인
            friendRequestRepository.findBySenderIdAndReceiverId(senderId.value, receiverId.value) != null
        }
    }

    private fun mapToDomain(
        entity: FriendRequestEntity
    ): FriendRequest {
        return FriendRequest(
            id = entity.id?.let { FriendRequestId.from(it) },
            senderId = UserId.from(entity.senderId),
            receiverId = UserId.from(entity.receiverId),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}
