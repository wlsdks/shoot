package com.stark.shoot.domain.saga.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.saga.SagaState
import java.util.*

/**
 * 메시지 저장 Saga의 컨텍스트
 *
 * Saga 실행 과정에서 필요한 모든 데이터를 담고 있습니다.
 */
data class MessageSagaContext(
    /**
     * Saga 고유 ID
     */
    val sagaId: String = UUID.randomUUID().toString(),

    /**
     * 저장할 메시지
     */
    val message: ChatMessage,

    /**
     * 채팅방 정보 (메타데이터 업데이트용)
     */
    var chatRoom: ChatRoom? = null,

    /**
     * MongoDB에 저장된 메시지 (rollback용)
     */
    var savedMessage: ChatMessage? = null,

    /**
     * PostgreSQL에서 업데이트된 채팅방 (rollback용)
     */
    var updatedChatRoom: ChatRoom? = null,

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
