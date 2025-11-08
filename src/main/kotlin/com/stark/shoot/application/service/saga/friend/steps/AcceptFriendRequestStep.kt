package com.stark.shoot.application.service.saga.friend.steps

import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.application.service.saga.friend.FriendRequestSagaContext
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.social.type.FriendRequestStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 1: 친구 요청 수락 (FriendRequest 상태 업데이트)
 *
 * @Transactional: PostgreSQL 트랜잭션 시작
 * Optimistic Locking: FriendRequestEntity에 @Version이 있어 동시 업데이트 시 OptimisticLockException 발생
 */
@Component
class AcceptFriendRequestStep(
    private val friendRequestQueryPort: FriendRequestQueryPort,
    private val friendRequestCommandPort: FriendRequestCommandPort
) : SagaStep<FriendRequestSagaContext> {

    private val logger = KotlinLogging.logger {}

    /**
     * FriendRequest 상태를 ACCEPTED로 변경
     *
     * OptimisticLockException은 FriendRequestSagaOrchestrator 레벨에서 처리됩니다.
     * 각 재시도마다 새로운 트랜잭션이 시작되므로 JPA 1차 캐시 문제가 해결됩니다.
     */
    @Transactional
    override fun execute(context: FriendRequestSagaContext): Boolean {
        return try {
            executeInternal(context)
        } catch (e: OptimisticLockException) {
            // Orchestrator 레벨에서 재시도하도록 예외를 context에 저장하고 실패 반환
            logger.warn { "OptimisticLockException occurred - will be retried by orchestrator" }
            context.markFailed(e)
            false
        } catch (e: Exception) {
            logger.error(e) { "Failed to accept friend request" }
            context.markFailed(e)
            false
        }
    }

    private fun executeInternal(context: FriendRequestSagaContext): Boolean {
        // 친구 요청 조회
        val friendRequest = friendRequestQueryPort.findRequest(
            senderId = context.requesterId,
            receiverId = context.receiverId,
            status = FriendRequestStatus.PENDING
        )

        if (friendRequest == null) {
            logger.error { "FriendRequest not found: requesterId=${context.requesterId.value}, receiverId=${context.receiverId.value}" }
            return false
        }

        // 보상용 스냅샷 저장
        context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
            requesterId = friendRequest.senderId.value,
            receiverId = friendRequest.receiverId.value,
            previousStatus = friendRequest.status,
            previousRespondedAt = friendRequest.respondedAt
        )

        // DDD Rich Model: FriendRequest.accept()가 Friendship + Event 생성
        val friendshipPair = friendRequest.accept()

        // Context에 FriendshipPair 저장 (Step 2, 3에서 사용)
        context.friendshipPair = friendshipPair

        // DB에 저장
        friendRequestCommandPort.saveFriendRequest(friendRequest)

        context.recordStep(stepName())
        logger.info { "FriendRequest accepted: requesterId=${context.requesterId.value}, receiverId=${context.receiverId.value}" }

        return true
    }

    /**
     * 보상: FriendRequest 상태를 PENDING으로 복원
     */
    @Transactional
    override fun compensate(context: FriendRequestSagaContext): Boolean {
        return try {
            val snapshot = context.friendRequestSnapshot

            if (snapshot == null) {
                logger.warn { "No friendRequest snapshot to restore - skipping compensation" }
                return true
            }

            // DB에서 최신 상태의 친구 요청을 다시 조회
            val friendRequest = friendRequestQueryPort.findRequest(
                senderId = context.requesterId,
                receiverId = context.receiverId,
                status = null  // 모든 상태의 요청 조회
            )

            if (friendRequest == null) {
                logger.warn { "FriendRequest not found for compensation: requesterId=${snapshot.requesterId}, receiverId=${snapshot.receiverId}" }
                return true  // 이미 삭제됨 - 보상 불필요
            }

            // 스냅샷 데이터로 이전 상태 복원
            friendRequest.status = snapshot.previousStatus
            friendRequest.respondedAt = snapshot.previousRespondedAt

            friendRequestCommandPort.saveFriendRequest(friendRequest)
            logger.info { "Compensated: Restored FriendRequest status to ${snapshot.previousStatus}: requesterId=${snapshot.requesterId}" }

            true
        } catch (e: OptimisticLockException) {
            // OptimisticLockException 발생 시 한 번 더 재시도
            logger.warn(e) { "OptimisticLockException during compensation - retrying once" }

            try {
                val snapshot = context.friendRequestSnapshot ?: return false
                val friendRequest = friendRequestQueryPort.findRequest(
                    senderId = context.requesterId,
                    receiverId = context.receiverId,
                    status = null
                ) ?: return true

                friendRequest.status = snapshot.previousStatus
                friendRequest.respondedAt = snapshot.previousRespondedAt

                friendRequestCommandPort.saveFriendRequest(friendRequest)
                logger.info { "Compensated after retry: requesterId=${snapshot.requesterId}" }
                true
            } catch (retryException: Exception) {
                logger.error(retryException) { "Compensation failed after retry - manual intervention required" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate friend request acceptance" }
            false
        }
    }

    override fun stepName() = "AcceptFriendRequest"
}
