package com.stark.shoot.application.port.out

/**
 * 알림 발송 포트 (Slack, Email 등)
 * Critical한 시스템 이벤트를 운영자에게 알림
 */
interface AlertNotificationPort {

    /**
     * Critical 알림을 발송합니다.
     *
     * @param alert 알림 정보
     */
    fun sendCriticalAlert(alert: CriticalAlert)
}

/**
 * Critical 알림 정보
 *
 * @property channel 알림 채널 (예: "#ops-critical", "ops-team@example.com")
 * @property title 알림 제목
 * @property message 알림 본문
 * @property level 알림 레벨 (ERROR, CRITICAL, WARNING)
 * @property metadata 추가 메타데이터
 */
data class CriticalAlert(
    val channel: String,
    val title: String,
    val message: String,
    val level: AlertLevel = AlertLevel.CRITICAL,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 알림 레벨
 */
enum class AlertLevel {
    WARNING,
    ERROR,
    CRITICAL
}
