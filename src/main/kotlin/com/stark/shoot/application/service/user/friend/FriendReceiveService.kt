package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.application.service.saga.friend.FriendRequestSagaOrchestrator
import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.domain.shared.event.FriendRequestRejectedEvent
import com.stark.shoot.domain.social.constants.FriendConstants
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.OptimisticLockException
import java.time.Instant

@UseCase
class FriendReceiveService(
    private val userQueryPort: UserQueryPort,
    private val friendRequestQueryPort: FriendRequestQueryPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val friendshipQueryPort: FriendshipQueryPort,
    private val eventPublisher: EventPublishPort,
    private val friendCacheManager: FriendCacheManager,
    private val friendRequestSagaOrchestrator: FriendRequestSagaOrchestrator,
    private val friendConstants: FriendConstants
) : FriendReceiveUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청을 수락합니다.
     *
     * DDD 개선: Saga Pattern 적용
     * - 여러 Aggregate 수정을 별도 트랜잭션으로 분리
     * - 실패 시 보상 트랜잭션 자동 실행
     * - OptimisticLockException 자동 재시도 (Orchestrator 레벨)
     *
     * @param command 친구 요청 수락 커맨드
     */
    override fun acceptFriendRequest(command: AcceptFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 사용자 존재 여부 확인
        validateUsers(currentUserId, requesterId)

        // 친구 수 제한 검증 (양쪽 사용자 모두 확인)
        val currentUserFriendCount = friendshipQueryPort.countByUserId(currentUserId)
        if (currentUserFriendCount >= friendConstants.maxFriendCount) {
            throw InvalidInputException("최대 친구 수를 초과했습니다. (최대: ${friendConstants.maxFriendCount}명)")
        }

        val requesterFriendCount = friendshipQueryPort.countByUserId(requesterId)
        if (requesterFriendCount >= friendConstants.maxFriendCount) {
            throw InvalidInputException("상대방이 최대 친구 수를 초과했습니다. (최대: ${friendConstants.maxFriendCount}명)")
        }

        // Saga 실행
        val context = friendRequestSagaOrchestrator.execute(requesterId, currentUserId)

        // Saga 실패 시 예외 발생
        if (context.state != SagaState.COMPLETED) {
            val errorMessage = context.error?.message ?: "친구 요청 수락 처리 중 오류가 발생했습니다."
            logger.error { "Friend request saga failed: sagaId=${context.sagaId}, error=$errorMessage" }
            throw InvalidInputException(errorMessage)
        }

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)

        logger.info { "Friend request accepted successfully: requesterId=${requesterId.value}, receiverId=${currentUserId.value}" }
    }

    /**
     * 친구 요청을 거절합니다.
     *
     * OptimisticLockException 발생 시 자동으로 최대 3번까지 재시도합니다.
     *
     * @param command 친구 요청 거절 커맨드
     */
    @Retryable(
        retryFor = [OptimisticLockException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
    )
    override fun rejectFriendRequest(command: RejectFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 사용자 존재 여부 확인
        validateUsers(currentUserId, requesterId)

        // 친구 요청 존재 여부 확인
        val friendRequest = friendRequestQueryPort
            .findRequest(requesterId, currentUserId, FriendRequestStatus.PENDING)
            ?: throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")

        // 친구 요청 상태 업데이트
        friendRequestCommandPort.updateStatus(requesterId, currentUserId, FriendRequestStatus.REJECTED)

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)

        // 친구 요청 거절 이벤트 발행 (트랜잭션 커밋 후 알림 전송 등 처리)
        publishFriendRequestRejectedEvent(requesterId, currentUserId)
    }


    /**
     * 사용자 존재 여부를 검증합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    private fun validateUsers(
        currentUserId: UserId,
        requesterId: UserId
    ) {
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }
        if (!userQueryPort.existsById(requesterId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")
        }
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
