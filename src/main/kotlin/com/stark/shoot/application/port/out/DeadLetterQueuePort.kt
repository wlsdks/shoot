package com.stark.shoot.application.port.out

import java.time.Instant

/**
 * Dead Letter Queue 발행 포트
 * Saga 보상 실패 등 복구 불가능한 오류를 DLQ로 전송
 */
interface DeadLetterQueuePort {

    /**
     * Dead Letter 메시지를 발행합니다.
     *
     * @param message Dead Letter 메시지
     */
    fun publish(message: DeadLetterMessage)
}

/**
 * Dead Letter Queue에 저장되는 메시지
 *
 * @property sagaId Saga 고유 ID
 * @property sagaType Saga 타입 (예: "MessageSaga", "FriendRequestSaga")
 * @property failedSteps 실패한 Step 목록
 * @property errorDetails 에러 상세 정보
 * @property payload 원본 컨텍스트 데이터 (JSON)
 * @property requiresManualIntervention 수동 개입 필요 여부
 * @property timestamp 발생 시각
 * @property retryCount 재시도 횟수 (선택)
 */
data class DeadLetterMessage(
    val sagaId: String,
    val sagaType: String,
    val failedSteps: List<String>,
    val errorDetails: String?,
    val payload: String,
    val requiresManualIntervention: Boolean = true,
    val timestamp: Instant = Instant.now(),
    val retryCount: Int = 0
)
