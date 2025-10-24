package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipCommandPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.domain.event.FriendRemovedEvent
import com.stark.shoot.domain.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRemoveService(
    private val userQueryPort: UserQueryPort,
    private val friendshipQueryPort: FriendshipQueryPort,
    private val friendshipCommandPort: FriendshipCommandPort,
    private val friendCacheManager: FriendCacheManager,
    private val eventPublisher: EventPublishPort
) : FriendRemoveUseCase {

    private val logger = KotlinLogging.logger {}

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

        // 도메인 이벤트 발행 (트랜잭션 커밋 후 리스너들이 처리)
        publishFriendRemovedEvent(userId, friendId)

        return user
    }

    /**
     * 친구 삭제 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 1:1 채팅방 처리, 알림 전송 등을 수행할 수 있습니다.
     */
    private fun publishFriendRemovedEvent(userId: com.stark.shoot.domain.user.vo.UserId, friendId: com.stark.shoot.domain.user.vo.UserId) {
        try {
            val event = FriendRemovedEvent.create(
                userId = userId,
                friendId = friendId
            )
            eventPublisher.publishEvent(event)
            logger.debug { "FriendRemovedEvent published for userId=${userId.value}, friendId=${friendId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish FriendRemovedEvent for userId=${userId.value}, friendId=${friendId.value}" }
        }
    }

}
