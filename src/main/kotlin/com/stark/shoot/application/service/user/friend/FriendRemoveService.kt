package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRemoveService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val redisStringTemplate: StringRedisTemplate,
    private val friendCachePort: FriendCachePort
) : FriendRemoveUseCase {

    private val maxPinnedFriends = 5

    override fun removeFriend(userId: Long, friendId: Long): User {
        // 기본 사용자 정보 조회
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        // 친구 관계 확인
        if (!findUserPort.checkFriendship(userId, friendId)) {
            // 이미 친구가 아니면 현재 상태 그대로 반환
            return user
        }

        // 친구 정보 조회
        val friend = findUserPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("User not found: $friendId")

        // 친구 관계 제거
        updateFriendPort.removeFriendRelation(userId, friendId)
        updateFriendPort.removeFriendRelation(friendId, userId)

        // 업데이트된 사용자 정보 조회
        val updatedUser = findUserPort.findUserWithFriendshipsById(userId)
            ?: throw ResourceNotFoundException("User not found after update: $userId")

        // 캐시 무효화
        invalidateRecommendationCache(userId)
        invalidateRecommendationCache(friendId)

        return updatedUser
    }

    /**
     * 추천 캐시 무효화
     */
    private fun invalidateRecommendationCache(userId: Long) {
        try {
            // 추천 친구 캐시 키 패턴
            val cacheKeyPattern = "friend_recommend:$userId:*"

            // 해당 패턴의 모든 키 조회
            val keys = redisStringTemplate.keys(cacheKeyPattern)

            // 키가 있으면 삭제
            if (!keys.isNullOrEmpty()) {
                redisStringTemplate.delete(keys)
            }

            // 친구 추천 캐시 무효화
            friendCachePort.invalidateUserCache(userId)
        } catch (e: Exception) {
            // 캐시 삭제 실패는 치명적인 오류가 아니므로 로깅만 하고 계속 진행
            println("캐시 삭제 실패: userId=$userId, error: ${e.message}")
        }
    }
}
