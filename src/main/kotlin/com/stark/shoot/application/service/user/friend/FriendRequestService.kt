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
    private val redisTemplate: StringRedisTemplate
) : FriendRequestUseCase {

    /**
     * 친구 요청을 보냅니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받을 사용자 ID
     */
    override fun sendFriendRequest(
        currentUserId: Long,
        targetUserId: Long
    ) {
        // 자기 자신에게 요청하는 경우 방지
        if (currentUserId == targetUserId) {
            throw InvalidInputException("자기 자신에게 친구 요청을 보낼 수 없습니다.")
        }

        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 이미 친구인지 확인
        if (findUserPort.checkFriendship(currentUserId, targetUserId)) {
            throw InvalidInputException("이미 친구 상태입니다.")
        }

        // 이미 친구 요청을 보냈는지 확인
        if (findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("이미 친구 요청을 보냈습니다.")
        }

        // 상대방으로부터 이미 친구 요청을 받은 상태인지 확인
        if (findUserPort.checkIncomingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("상대방이 이미 친구 요청을 보냈습니다. 수락하거나 거절해주세요.")
        }

        // 친구 요청 추가
        updateFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)

        // 추천 친구 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(targetUserId)
    }

    /**
     * 친구 요청을 수락합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun acceptFriendRequest(
        currentUserId: Long,
        requesterId: Long
    ) {
        // 사용자 조회
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        val requester = findUserPort.findUserById(requesterId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")

        // 친구 요청 존재 여부 확인
        if (!currentUser.incomingFriendRequestIds.contains(requesterId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 도메인 객체의 메서드를 사용하여 친구 요청 수락
        val updatedCurrentUser = currentUser.acceptFriendRequest(requesterId)
        updateFriendPort.updateFriends(updatedCurrentUser)

        // 요청자의 친구 목록에도 추가
        val updatedRequester = requester.addFriend(currentUserId)
        updateFriendPort.updateFriends(updatedRequester)

        // 이벤트 발행 (양쪽 사용자에게 친구 추가 알림)
        eventPublisher.publish(FriendAddedEvent(userId = currentUserId, friendId = requesterId))
        eventPublisher.publish(FriendAddedEvent(userId = requesterId, friendId = currentUserId))

        // 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(requesterId)
    }

    /**
     * 친구 요청을 거절합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun rejectFriendRequest(
        currentUserId: Long,
        requesterId: Long
    ) {
        // 사용자 조회
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        val requester = findUserPort.findUserById(requesterId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")

        // 친구 요청 존재 여부 확인
        if (!currentUser.incomingFriendRequestIds.contains(requesterId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 도메인 객체의 메서드를 사용하여 친구 요청 거절
        val updatedCurrentUser = currentUser.rejectFriendRequest(requesterId)
        updateFriendPort.updateFriends(updatedCurrentUser)

        // 요청자의 발신 요청 목록에서도 제거
        val updatedRequester = requester.cancelFriendRequest(currentUserId)
        updateFriendPort.updateFriends(updatedRequester)

        // 캐시 무효화
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(requesterId)
    }

    /**
     * 추천 친구 캐시를 무효화합니다.
     *
     * @param userId 사용자 ID
     */
    private fun invalidateRecommendationCache(userId: Long) {
        try {
            // 추천 친구 캐시 키 패턴
            val cacheKeyPattern = "friend_recommend:$userId:*"

            // 해당 패턴의 모든 키 조회
            val keys = redisTemplate.keys(cacheKeyPattern)

            // 키가 있으면 삭제
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
            }
        } catch (e: Exception) {
            // 캐시 삭제 실패는 치명적인 오류가 아니므로 로깅만 하고 계속 진행
            // 실제 구현 시 로깅 추가 필요
        }
    }

}
