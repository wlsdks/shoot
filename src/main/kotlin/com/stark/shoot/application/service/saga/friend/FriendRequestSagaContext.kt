package com.stark.shoot.application.service.saga.friend

import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.social.type.FriendRequestStatus
import java.time.Instant
import java.util.*

/**
 * 친구 요청 수락 Saga의 컨텍스트
 *
 * DDD 개선: 도메인 객체 제거, ID와 primitive 타입만 포함
 * Saga는 Application Layer이므로 필요 시 도메인 객체를 조회하여 사용
 */
data class FriendRequestSagaContext(
    /**
     * Saga 고유 ID
     */
    val sagaId: String = UUID.randomUUID().toString(),

    /**
     * 친구 요청을 보낸 사용자 ID
     */
    val requesterId: UserId,

    /**
     * 친구 요청을 받은 사용자 ID (현재 사용자)
     */
    val receiverId: UserId,

    /**
     * 보상 트랜잭션용 FriendRequest 스냅샷
     */
    var friendRequestSnapshot: FriendRequestSnapshot? = null,

    /**
     * 생성된 Friendship ID 목록 (보상 시 삭제용)
     */
    val createdFriendshipIds: MutableList<Long> = mutableListOf(),

    /**
     * Saga 상태
     */
    var state: SagaState = SagaState.STARTED,

    /**
     * 실행된 단계 목록 (역순 보상용)
     */
    val executedSteps: MutableList<String> = mutableListOf(),

    /**
     * 에러 정보
     */
    var error: Throwable? = null
) {
    /**
     * FriendRequest 스냅샷 (보상용)
     */
    data class FriendRequestSnapshot(
        val requesterId: Long,
        val receiverId: Long,
        val previousStatus: FriendRequestStatus,
        val previousRespondedAt: Instant?
    )

    /**
     * 단계 실행 기록
     */
    fun recordStep(stepName: String) {
        executedSteps.add(stepName)
    }

    /**
     * Saga 완료 처리
     */
    fun markCompleted() {
        state = SagaState.COMPLETED
    }

    /**
     * 보상 시작
     */
    fun startCompensation(throwable: Throwable) {
        state = SagaState.COMPENSATING
        error = throwable
    }

    /**
     * 보상 완료
     */
    fun markCompensated() {
        state = SagaState.COMPENSATED
    }

    /**
     * 실패 처리
     */
    fun markFailed(throwable: Throwable) {
        state = SagaState.FAILED
        error = throwable
    }
}
