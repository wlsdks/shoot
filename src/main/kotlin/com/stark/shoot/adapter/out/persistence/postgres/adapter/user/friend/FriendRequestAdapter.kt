package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.FriendRequestPort
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.FriendRequestId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@Adapter
class FriendRequestAdapter(
    private val friendRequestRepository: FriendRequestRepository,
    private val userRepository: UserRepository
) : FriendRequestPort {

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

    override fun createRequest(friendRequest: FriendRequest): FriendRequest {
        val sender = userRepository.findById(friendRequest.senderId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendRequest.senderId.value}") }

        val receiver = userRepository.findById(friendRequest.receiverId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendRequest.receiverId.value}") }

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

    override fun updateRequest(friendRequest: FriendRequest): FriendRequest {
        val entity = friendRequest.id?.let {
            friendRequestRepository.findById(it.value).orElseThrow {
                ResourceNotFoundException("친구 요청을 찾을 수 없습니다: $it")
            }
        } ?: throw IllegalArgumentException("친구 요청 ID가 없습니다.")

        entity.status = friendRequest.status
        entity.respondedAt = friendRequest.respondedAt

        val savedEntity = friendRequestRepository.save(entity)
        return mapToDomain(savedEntity)
    }

    private fun mapToDomain(entity: FriendRequestEntity): FriendRequest {
        return FriendRequest(
            id = entity.id?.let { FriendRequestId.from(it) },
            senderId = UserId.from(entity.sender.id),
            receiverId = UserId.from(entity.receiver.id),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}
