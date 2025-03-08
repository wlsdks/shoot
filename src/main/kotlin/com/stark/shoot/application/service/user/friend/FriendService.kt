package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate

@UseCase
class FriendService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val eventPublisher: EventPublisher,
    private val redisStringTemplate: StringRedisTemplate
) : FriendUseCase {

    // 추천 시스템에서 사용하는 최대 깊이 (예: 2)
    private val maxDepth = 2

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
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")

        val targetUser = findUserPort.findUserById(targetUserId)
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
        updateFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)
        updateFriendPort.addIncomingFriendRequest(targetUserId, currentUserId)

        // 친구 관계 변경으로 인해 추천 캐시 무효화 (양쪽 모두)
        invalidateRecommendationCacheForUser(currentUserId)
        invalidateRecommendationCacheForUser(targetUserId)
    }

    /**
     * 친구 요청 수락
     */
    override fun acceptFriendRequest(
        currentUserId: ObjectId,
        requesterId: ObjectId
    ) {
        // currentUserId = 수락하는 사람, requesterId = 요청 보낸 사람
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")
        if (!currentUser.incomingFriendRequests.contains(requesterId)) {
            throw InvalidInputException("해당 요청이 없습니다.")
        }

        // 요청 보낸 쪽에서 outgoing에서 제거 + 친구 추가
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)

        // 서로 friends에 추가
        updateFriendPort.addFriendRelation(currentUserId, requesterId)
        updateFriendPort.addFriendRelation(requesterId, currentUserId)

        // SSE 이벤트 발행
        eventPublisher.publish(FriendAddedEvent(userId = currentUserId.toString(), friendId = requesterId.toString()))
        eventPublisher.publish(FriendAddedEvent(userId = requesterId.toString(), friendId = currentUserId.toString()))

        // 친구 관계 변경으로 인해 추천 캐시 무효화 (양쪽 모두)
        invalidateRecommendationCacheForUser(currentUserId)
        invalidateRecommendationCacheForUser(requesterId)
    }

    /**
     * 친구 요청 거절
     */
    override fun rejectFriendRequest(
        currentUserId: ObjectId,
        requesterId: ObjectId
    ) {
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")
        if (!currentUser.incomingFriendRequests.contains(requesterId)) {
            throw InvalidInputException("해당 요청이 없습니다.")
        }

        // 요청 보낸 쪽에서 outgoing에서 제거
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)

        // 친구 관계 변경으로 인해 추천 캐시 무효화 (양쪽 모두)
        invalidateRecommendationCacheForUser(currentUserId)
        invalidateRecommendationCacheForUser(requesterId)
    }

    /**
     * 친구 삭제
     *
     * @param userId 현재 사용자 ID
     * @param friendId 삭제할 친구 ID
     * @return 업데이트된 사용자 정보
     */
    override fun removeFriend(
        userId: ObjectId,
        friendId: ObjectId
    ): User {
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        val friend = findUserPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("User not found: $friendId")

        // 친구 관계 삭제
        val updatedUser = user.copy(friends = user.friends - friendId)
        val updatedFriend = friend.copy(friends = friend.friends - userId)

        // 친구 관계 업데이트
        updateFriendPort.updateFriends(updatedFriend)
        val updateFriends = updateFriendPort.updateFriends(updatedUser)

        // 친구 관계 변경으로 인해 추천 캐시 무효화 (양쪽 모두)
        invalidateRecommendationCacheForUser(userId)
        invalidateRecommendationCacheForUser(friendId)

        return updateFriends
    }

    /**
     * 친구 추천 캐시 무효화 로직.
     * 캐시 키는 "friend_recommend:{userId}:{maxDepth}" 형식으로 구성됨.
     */
    private fun invalidateRecommendationCacheForUser(userId: ObjectId) {
        val cacheKey = "friend_recommend:${userId}:$maxDepth"
        try {
            redisStringTemplate.delete(cacheKey)
            // 로컬 캐시를 사용 중이라면 여기서도 제거
        } catch (e: Exception) {
            // 로그 기록 등 처리 (예: 로깅 라이브러리 사용)
            println("캐시 삭제 실패: $cacheKey, error: ${e.message}")
        }
    }

}