package com.stark.shoot.domain.saga.message

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.domain.shared.UserId
import java.time.Instant
import java.util.*

/**
 * 메시지 저장 Saga의 컨텍스트
 *
 * DDD 개선: 도메인 객체 제거, ID와 primitive 타입만 포함
 * Saga는 Application Layer이므로 필요 시 도메인 객체를 조회하여 사용
 */
data class MessageSagaContext(
    /**
     * Saga 고유 ID
     */
    val sagaId: String = UUID.randomUUID().toString(),

    /**
     * 저장할 메시지 ID (저장 전에는 null)
     */
    val messageId: MessageId?,

    /**
     * 채팅방 ID
     */
    val roomId: ChatRoomId,

    /**
     * 발신자 ID
     */
    val senderId: UserId,

    /**
     * MongoDB에 저장된 메시지 ID (rollback용)
     */
    var savedMessageId: String? = null,

    /**
     * 보상 트랜잭션용 채팅방 스냅샷
     */
    var chatRoomSnapshot: ChatRoomSnapshot? = null,

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
     * 채팅방 스냅샷 (보상용)
     */
    data class ChatRoomSnapshot(
        val roomId: Long,
        val previousLastMessageId: String?,
        val previousLastActiveAt: Instant
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
