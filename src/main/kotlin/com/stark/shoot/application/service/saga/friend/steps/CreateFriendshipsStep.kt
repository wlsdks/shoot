package com.stark.shoot.application.service.saga.friend.steps

import com.stark.shoot.application.port.out.user.friend.FriendCommandPort
import com.stark.shoot.application.service.saga.friend.FriendRequestSagaContext
import com.stark.shoot.domain.saga.SagaStep
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.OptimisticLockException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Saga Step 2: 양방향 친구 관계 생성
 *
 * @Transactional: PostgreSQL 트랜잭션 시작
 * 2개의 Friendship 레코드를 생성합니다 (양방향 관계)
 */
@Component
class CreateFriendshipsStep(
    private val friendCommandPort: FriendCommandPort
) : SagaStep<FriendRequestSagaContext> {

    private val logger = KotlinLogging.logger {}

    /**
     * 양방향 친구 관계 생성
     * - userId=receiverId, friendId=requesterId
     * - userId=requesterId, friendId=receiverId
     *
     * OptimisticLockException은 FriendRequestSagaOrchestrator 레벨에서 처리됩니다.
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
            logger.error(e) { "Failed to create friendships" }
            context.markFailed(e)
            false
        }
    }

    private fun executeInternal(context: FriendRequestSagaContext): Boolean {
        // 양방향 친구 관계 생성
        // 1. receiverId → requesterId
        friendCommandPort.addFriendRelation(
            userId = context.receiverId,
            friendId = context.requesterId
        )

        // 2. requesterId → receiverId
        friendCommandPort.addFriendRelation(
            userId = context.requesterId,
            friendId = context.receiverId
        )

        context.recordStep(stepName())
        logger.info {
            "Friendships created: " +
                    "user1=${context.receiverId.value}, " +
                    "user2=${context.requesterId.value}"
        }

        return true
    }

    /**
     * 보상: 생성된 양방향 친구 관계 삭제
     */
    @Transactional
    override fun compensate(context: FriendRequestSagaContext): Boolean {
        return try {
            // 양방향 친구 관계 삭제
            // 1. receiverId → requesterId 삭제
            friendCommandPort.removeFriendRelation(
                userId = context.receiverId,
                friendId = context.requesterId
            )

            // 2. requesterId → receiverId 삭제
            friendCommandPort.removeFriendRelation(
                userId = context.requesterId,
                friendId = context.receiverId
            )

            logger.info {
                "Compensated: Deleted friendships: " +
                        "user1=${context.receiverId.value}, " +
                        "user2=${context.requesterId.value}"
            }

            true
        } catch (e: OptimisticLockException) {
            // OptimisticLockException 발생 시 한 번 더 재시도
            logger.warn(e) { "OptimisticLockException during compensation - retrying once" }

            try {
                friendCommandPort.removeFriendRelation(
                    userId = context.receiverId,
                    friendId = context.requesterId
                )
                friendCommandPort.removeFriendRelation(
                    userId = context.requesterId,
                    friendId = context.receiverId
                )

                logger.info { "Compensated after retry: deleted friendships" }
                true
            } catch (retryException: Exception) {
                logger.error(retryException) { "Compensation failed after retry - manual intervention required" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to compensate friendship creation" }
            false
        }
    }

    override fun stepName() = "CreateFriendships"
}
