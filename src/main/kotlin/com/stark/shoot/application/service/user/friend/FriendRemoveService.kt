package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendshipPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRemoveService(
    private val findUserPort: FindUserPort,
    private val friendshipPort: FriendshipPort,
    private val friendCacheManager: FriendCacheManager
) : FriendRemoveUseCase {

    override fun removeFriend(command: RemoveFriendCommand): User {
        val userId = command.userId
        val friendId = command.friendId

        // 기본 사용자 정보 조회
        val user = findUserPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        // 친구 관계 확인
        if (!friendshipPort.isFriend(userId, friendId)) {
            // 이미 친구가 아니면 현재 상태 그대로 반환
            return user
        }

        // 친구 정보 조회
        val friend = findUserPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("User not found: $friendId")

        // 친구 관계 제거
        friendshipPort.removeFriendship(userId, friendId)
        friendshipPort.removeFriendship(friendId, userId)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(userId, friendId)

        return user
    }

}
