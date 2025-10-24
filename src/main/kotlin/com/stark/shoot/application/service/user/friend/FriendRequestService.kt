package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.CancelFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestFromCodeCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.domain.event.FriendRequestCancelledEvent
import com.stark.shoot.domain.event.FriendRequestSentEvent
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
class FriendRequestService(
    private val userQueryPort: UserQueryPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val friendDomainService: FriendDomainService,
    private val friendCacheManager: FriendCacheManager,
    private val eventPublisher: EventPublishPort
) : FriendRequestUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청 처리를 위한 공통 로직을 수행하는 내부 메서드
     *
     * @param currentUserId 요청을 보내는 사용자 ID
     * @param targetUserId 요청을 받는 사용자 ID
     */
    private fun processFriendRequest(currentUserId: UserId, targetUserId: UserId) {
        // 사용자 존재 여부 확인
        validateUserExistence(currentUserId, targetUserId)

        // 도메인 서비스를 사용하여 친구 요청 유효성 검증
        try {
            friendDomainService.validateFriendRequest(
                currentUserId = currentUserId,
                targetUserId = targetUserId,
                isFriend = userQueryPort.checkFriendship(currentUserId, targetUserId),
                hasOutgoingRequest = userQueryPort.checkOutgoingFriendRequest(
                    currentUserId,
                    targetUserId
                ),
                hasIncomingRequest = userQueryPort.checkIncomingFriendRequest(
                    currentUserId,
                    targetUserId
                )
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(e.message ?: "친구 요청 유효성 검증 실패")
        }

        // 친구 요청 애그리게이트 생성 및 저장
        val request = FriendRequest(senderId = currentUserId, receiverId = targetUserId)
        friendRequestCommandPort.saveFriendRequest(request)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(currentUserId, targetUserId)

        // 친구 요청 전송 이벤트 발행 (트랜잭션 커밋 후 알림 전송 등 처리)
        publishFriendRequestSentEvent(currentUserId, targetUserId)
    }

    override fun sendFriendRequest(command: SendFriendRequestCommand) {
        processFriendRequest(command.currentUserId, command.targetUserId)
    }

    /**
     * 친구 요청을 취소합니다.
     *
     * @param command 친구 요청 취소 커맨드
     */
    override fun cancelFriendRequest(command: CancelFriendRequestCommand) {
        // 사용자 존재 여부 확인
        validateUserExistence(command.currentUserId, command.targetUserId)

        // 친구 요청 존재 여부 확인
        if (!userQueryPort.checkOutgoingFriendRequest(command.currentUserId, command.targetUserId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 요청 상태를 취소로 변경
        friendRequestCommandPort.updateStatus(
            command.currentUserId,
            command.targetUserId,
            FriendRequestStatus.CANCELLED
        )

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(command.currentUserId, command.targetUserId)

        // 친구 요청 취소 이벤트 발행
        publishFriendRequestCancelledEvent(command.currentUserId, command.targetUserId)
    }

    override fun sendFriendRequestFromUserCode(command: SendFriendRequestFromCodeCommand) {
        processFriendRequest(command.currentUserId, command.targetUserId)
    }


    /**
     * 두 사용자의 존재 여부를 확인합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받을 사용자 ID
     */
    private fun validateUserExistence(
        currentUserId: UserId,
        targetUserId: UserId
    ) {
        // 두 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }
        if (!userQueryPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }
    }

    /**
     * 친구 요청 전송 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 알림 전송 등의 처리를 수행할 수 있습니다.
     */
    private fun publishFriendRequestSentEvent(senderId: UserId, receiverId: UserId) {
        try {
            val event = FriendRequestSentEvent.create(
                senderId = senderId,
                receiverId = receiverId,
                sentAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "FriendRequestSentEvent published: sender=${senderId.value}, receiver=${receiverId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish FriendRequestSentEvent: sender=${senderId.value}, receiver=${receiverId.value}" }
        }
    }

    /**
     * 친구 요청 취소 이벤트를 발행합니다.
     */
    private fun publishFriendRequestCancelledEvent(senderId: UserId, receiverId: UserId) {
        try {
            val event = FriendRequestCancelledEvent.create(
                senderId = senderId,
                receiverId = receiverId,
                cancelledAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "FriendRequestCancelledEvent published: sender=${senderId.value}, receiver=${receiverId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish FriendRequestCancelledEvent: sender=${senderId.value}, receiver=${receiverId.value}" }
        }
    }

}
