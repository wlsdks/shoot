package com.stark.shoot.application.port.out.notification

/**
 * Slack 알림 전송 Port
 *
 * DLQ 이벤트, Saga 실패 등 중요한 운영 알림을 Slack으로 전송합니다.
 */
interface SlackNotificationPort {

    /**
     * DLQ 이벤트 알림
     *
     * @param sagaId Saga ID
     * @param eventType 이벤트 타입
     * @param failureReason 실패 원인
     */
    fun notifyDLQEvent(sagaId: String, eventType: String, failureReason: String)

    /**
     * 미해결 DLQ 알림
     *
     * @param unresolvedCount 미해결 DLQ 개수
     * @param recentDLQInfo 최근 DLQ 정보
     */
    fun notifyUnresolvedDLQ(unresolvedCount: Long, recentDLQInfo: String)

    /**
     * Saga 실패 알림
     *
     * @param sagaId Saga ID
     * @param errorMessage 에러 메시지
     */
    fun notifySagaFailure(sagaId: String, errorMessage: String)

    /**
     * 일반 에러 알림
     *
     * @param title 제목
     * @param message 메시지
     */
    fun notifyError(title: String, message: String)
}
