package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.ManageFriendUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.application.port.out.user.UpdateUserFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.common.exception.InvalidInputException
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ManageFriendService(
    private val retrieveUserPort: RetrieveUserPort,
    private val updateUserFriendPort: UpdateUserFriendPort,
) : ManageFriendUseCase {

    /**
     * 친구 목록 조회
     */
    override fun getFriends(
        currentUserId: ObjectId
    ): List<ObjectId> {
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")
        return user.friends.toList()
    }

    /**
     * 받은 친구 요청 목록 조회
     */
    override fun getIncomingFriendRequests(
        currentUserId: ObjectId
    ): List<ObjectId> {
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")
        return user.incomingFriendRequests.toList()
    }

    /**
     * 보낸 친구 요청 목록 조회
     */
    override fun getOutgoingFriendRequests(
        currentUserId: ObjectId
    ): List<ObjectId> {
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")
        return user.outgoingFriendRequests.toList()
    }

    /**
     * 친구 요청 보내기
     */
    override fun sendFriendRequest(
        currentUserId: ObjectId,
        targetUserId: ObjectId
    ) {
        if (currentUserId == targetUserId) {
            throw InvalidInputException("자기 자신에게 친구 요청 불가")
        }
        // 요청 보낸 유저, 받는 유저 둘 다 DB에 있는지 검사
        val currentUser = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")
        val targetUser = retrieveUserPort.findById(targetUserId)
            ?: throw ResourceNotFoundException("User not found: $targetUserId")

        // 이미 친구? 이미 보냈거나 받은 요청? 등 체크
        if (currentUser.friends.contains(targetUserId)) {
            throw InvalidInputException("이미 친구 상태입니다.")
        }
        if (currentUser.outgoingFriendRequests.contains(targetUserId)) {
            throw InvalidInputException("이미 친구 요청을 보냈습니다.")
        }
        if (currentUser.incomingFriendRequests.contains(targetUserId)) {
            throw InvalidInputException("상대방이 이미 당신에게 요청을 보냈습니다. 수락/거절을 해주세요.")
        }

        // 양쪽에 pending 상태로 저장
        updateUserFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)
        updateUserFriendPort.addIncomingFriendRequest(targetUserId, currentUserId)
    }

    /**
     * 친구 요청 수락
     */
    override fun acceptFriendRequest(
        currentUserId: ObjectId,
        requesterId: ObjectId
    ) {
        // currentUserId = 수락하는 사람, requesterId = 요청 보낸 사람
        val currentUser = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")
        if (!currentUser.incomingFriendRequests.contains(requesterId)) {
            throw InvalidInputException("해당 요청이 없습니다.")
        }

        // 요청 보낸 쪽에서 outgoing에서 제거 + 친구 추가
        updateUserFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateUserFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)

        // 서로 friends에 추가
        updateUserFriendPort.addFriendRelation(currentUserId, requesterId)
        updateUserFriendPort.addFriendRelation(requesterId, currentUserId)
    }

    /**
     * 친구 요청 거절
     */
    override fun rejectFriendRequest(
        currentUserId: ObjectId,
        requesterId: ObjectId
    ) {
        val currentUser = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")
        if (!currentUser.incomingFriendRequests.contains(requesterId)) {
            throw InvalidInputException("해당 요청이 없습니다.")
        }

        // 요청 보낸 쪽에서 outgoing에서 제거
        updateUserFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateUserFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)
    }

    /**
     * 친구 요청 거절
     */
    override fun searchPotentialFriends(
        currentUserId: ObjectId,
        query: String
    ): List<User> {
        // 현재 사용자 조회
        val currentUser = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")

        // 제외할 사용자 목록: 본인, 이미 친구, 받은/보낸 친구 요청 대상
        val excludedIds = mutableSetOf<ObjectId>().apply {
            add(currentUserId)
            addAll(currentUser.friends)
            addAll(currentUser.incomingFriendRequests)
            addAll(currentUser.outgoingFriendRequests)
        }

        // 모든 사용자 조회 (대규모 DB의 경우 효율적 쿼리로 대체 필요)
        val allUsers = retrieveUserPort.findAll()

        // username 또는 nickname에 검색어(query)가 포함된 사용자 필터링 (대소문자 무시)
        return allUsers.filter { user ->
            user.id != null &&
                    !excludedIds.contains(user.id) &&
                    (user.username.contains(query, ignoreCase = true) ||
                            user.nickname.contains(query, ignoreCase = true))
        }
    }

}