package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
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
    private val redisTemplate: StringRedisTemplate,
    private val friendCachePort: FriendCachePort,
    private val friendDomainService: com.stark.shoot.domain.service.user.FriendDomainService
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
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 도메인 서비스를 사용하여 친구 요청 유효성 검증
        try {
            friendDomainService.validateFriendRequest(
                currentUserId = currentUserId,
                targetUserId = targetUserId,
                isFriend = findUserPort.checkFriendship(currentUserId, targetUserId),
                hasOutgoingRequest = findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId),
                hasIncomingRequest = findUserPort.checkIncomingFriendRequest(currentUserId, targetUserId)
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(e.message ?: "친구 요청 유효성 검증 실패")
        }

        // 친구 요청 추가
        updateFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)

        // 추천 친구 캐시 무효화 (Redis 및 로컬 캐시)
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(targetUserId)

        // 친구 추천 캐시 무효화
        friendCachePort.invalidateUserCache(currentUserId)
        friendCachePort.invalidateUserCache(targetUserId)
    }

    /**
     * 친구 요청을 취소합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받은 사용자 ID
     */
    override fun cancelFriendRequest(
        currentUserId: Long,
        targetUserId: Long
    ) {
        // 사용자 조회 (친구 요청 정보 포함)
        val currentUser = findUserPort.findUserWithFriendRequestsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        val targetUser = findUserPort.findUserById(targetUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")

        // 친구 요청 존재 여부 확인
        if (!findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 도메인 서비스를 사용하여 친구 요청 거절 처리
        val result = friendDomainService.processFriendReject(
            currentUser = targetUser, // 대상 사용자가 현재 사용자의 요청을 거절하는 것과 동일
            requester = currentUser,  // 현재 사용자가 요청자
            requesterId = currentUserId
        )

        // 업데이트된 사용자 정보 저장
        updateFriendPort.updateFriends(result.updatedCurrentUser)
        updateFriendPort.updateFriends(result.updatedRequester)

        // 실제 친구 요청 데이터 삭제
        updateFriendPort.removeOutgoingFriendRequest(currentUserId, targetUserId)

        // 캐시 무효화 (Redis 및 로컬 캐시)
        invalidateRecommendationCache(currentUserId)
        invalidateRecommendationCache(targetUserId)

        // 친구 추천 캐시 무효화
        friendCachePort.invalidateUserCache(currentUserId)
        friendCachePort.invalidateUserCache(targetUserId)
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
