package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipCommandPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRemoveService(
    private val userQueryPort: UserQueryPort,
    private val friendshipQueryPort: FriendshipQueryPort,
    private val friendshipCommandPort: FriendshipCommandPort,
    private val friendCacheManager: FriendCacheManager
) : FriendRemoveUseCase {

    override fun removeFriend(command: RemoveFriendCommand): User {
        val userId = command.userId
        val friendId = command.friendId

        // 기본 사용자 정보 조회
        val user = userQueryPort.findUserById(userId)
            ?: throw ResourceNotFoundException("User not found: $userId")

        // 친구 관계 확인
        if (!friendshipQueryPort.isFriend(userId, friendId)) {
            // 이미 친구가 아니면 현재 상태 그대로 반환
            return user
        }

        // 친구 정보 조회
        val friend = userQueryPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("User not found: $friendId")

        // 친구 관계 제거
        friendshipCommandPort.removeFriendship(userId, friendId)
        friendshipCommandPort.removeFriendship(friendId, userId)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(userId, friendId)

        return user
    }

}
