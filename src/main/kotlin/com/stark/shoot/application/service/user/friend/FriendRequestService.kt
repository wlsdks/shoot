package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRequestService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val eventPublisher: EventPublisher,
    private val redisStringTemplate: StringRedisTemplate
) : FriendRequestUseCase {

    private val maxPinnedFriends = 5

    /**
     * 친구 요청을 보냅니다.
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받을 사용자 ID
     */
    override fun sendFriendRequest(
        currentUserId: Long,
        targetUserId: Long
    ) {
        if (currentUserId == targetUserId) {
            throw InvalidInputException("자기 자신에게 친구 요청을 보낼 수 없습니다.")
        }

        // 사용자 존재 확인만 - 객체 로드 없이
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("User not found: $targetUserId")
        }

        // 효율적인 관계 확인
        if (findUserPort.checkFriendship(currentUserId, targetUserId)) {
            throw InvalidInputException("이미 친구 상태입니다.")
        }

        if (findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("이미 친구 요청을 보냈습니다.")
        }

        if (findUserPort.checkIncomingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("상대방이 이미 친구 요청을 보냈습니다. 수락하거나 거절해주세요.")
        }

        // 친구 요청 추가
        updateFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)

        // 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(targetUserId)
    }

    /**
     * 친구 요청을 수락합니다.
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun acceptFriendRequest(
        currentUserId: Long,
        requesterId: Long
    ) {
        // 친구 요청 확인 - 단일 쿼리로 확인
        if (!findUserPort.checkIncomingFriendRequest(currentUserId, requesterId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 요청 목록에서 제거
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)

        // 친구 관계 추가
        updateFriendPort.addFriendRelation(currentUserId, requesterId)
        updateFriendPort.addFriendRelation(requesterId, currentUserId)

        // 이벤트 발행
        eventPublisher.publish(FriendAddedEvent(userId = currentUserId.toString(), friendId = requesterId.toString()))
        eventPublisher.publish(FriendAddedEvent(userId = requesterId.toString(), friendId = currentUserId.toString()))

        // 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(requesterId)
    }

    /**
     * 친구 요청을 거절합니다.
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun rejectFriendRequest(
        currentUserId: Long,
        requesterId: Long
    ) {
        // 친구 요청 확인 - 단일 쿼리로 확인
        if (!findUserPort.checkIncomingFriendRequest(currentUserId, requesterId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 요청 목록에서 제거
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)

        // 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(requesterId)
    }

    /**
     * 추천 캐시 무효화
     */
    private fun invalidateRecommendationCache(userId: Long) {
        val cacheKey = "friend_recommend:$userId:$maxPinnedFriends"
        try {
            redisStringTemplate.delete(cacheKey)
        } catch (e: Exception) {
            println("캐시 삭제 실패: $cacheKey, error: ${e.message}")
        }
    }

}