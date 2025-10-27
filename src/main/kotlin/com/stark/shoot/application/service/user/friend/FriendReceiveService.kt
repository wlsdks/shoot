package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.application.port.out.user.friend.FriendCommandPort
import com.stark.shoot.domain.event.FriendRequestRejectedEvent
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.service.FriendDomainService
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class FriendReceiveService(
    private val userQueryPort: UserQueryPort,
    private val friendCommandPort: FriendCommandPort,
    private val friendRequestQueryPort: FriendRequestQueryPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val eventPublisher: EventPublishPort,
    private val friendDomainService: FriendDomainService,
    private val friendCacheManager: FriendCacheManager
) : FriendReceiveUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청을 수락합니다.
     *
     * @param command 친구 요청 수락 커맨드
     */
    override fun acceptFriendRequest(command: AcceptFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 친구 요청 조회 및 유효성 검사
        val friendRequest = findFriendRequest(currentUserId, requesterId)

        // 도메인 서비스를 사용하여 친구 요청 수락 처리
        val result = friendDomainService.processFriendAccept(friendRequest)

        // 친구 요청 상태 업데이트
        friendRequestCommandPort.updateStatus(requesterId, currentUserId, FriendRequestStatus.ACCEPTED)

        // 친구 관계 생성 (도메인 서비스에서 생성된 Friendship 사용)
        result.friendships.forEach { friendship ->
            friendCommandPort.addFriendRelation(friendship.userId, friendship.friendId)
        }

        // 이벤트 발행
        result.events.forEach { event -> eventPublisher.publishEvent(event) }

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)
    }

    /**
     * 친구 요청을 거절합니다.
     *
     * @param command 친구 요청 거절 커맨드
     */
    override fun rejectFriendRequest(command: RejectFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 친구 요청 조회 및 유효성 검사
        findFriendRequest(currentUserId, requesterId)

        // 친구 요청 상태 업데이트
        friendRequestCommandPort.updateStatus(requesterId, currentUserId, FriendRequestStatus.REJECTED)

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)

        // 친구 요청 거절 이벤트 발행 (트랜잭션 커밋 후 알림 전송 등 처리)
        publishFriendRequestRejectedEvent(requesterId, currentUserId)
    }


    /**
     * 친구 요청을 조회합니다. (유효성 검사 포함)
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     * @return 친구 요청 정보
     */
    private fun findFriendRequest(
        currentUserId: UserId,
        requesterId: UserId
    ): FriendRequest {
        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }
        if (!userQueryPort.existsById(requesterId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")
        }

        // 친구 요청 조회
        return friendRequestQueryPort
            .findRequest(requesterId, currentUserId, FriendRequestStatus.PENDING)
            ?: throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
    }

    /**
     * 친구 요청 거절 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 알림 전송 등의 처리를 수행할 수 있습니다.
     */
    private fun publishFriendRequestRejectedEvent(senderId: UserId, receiverId: UserId) {
        try {
            val event = FriendRequestRejectedEvent.create(
                senderId = senderId,
                receiverId = receiverId,
                rejectedAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "FriendRequestRejectedEvent published: sender=${senderId.value}, receiver=${receiverId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish FriendRequestRejectedEvent: sender=${senderId.value}, receiver=${receiverId.value}" }
        }
    }

}
