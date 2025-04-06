package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.RemoveFriendUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class RemoveFriendService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val redisStringTemplate: StringRedisTemplate
) : RemoveFriendUseCase {

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
        val cacheKey = "friend_recommend:$userId:$maxPinnedFriends"
        try {
            redisStringTemplate.delete(cacheKey)
        } catch (e: Exception) {
            println("캐시 삭제 실패: $cacheKey, error: ${e.message}")
        }
    }
}