package com.stark.shoot.adapter.out.notification.slack

import com.stark.shoot.application.port.out.notification.SlackNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * No-Op Slack Notification Adapter
 *
 * Slack이 비활성화된 경우 사용되는 구현체입니다.
 * 실제로 알림을 전송하지 않고 로그만 남깁니다.
 *
 * **활성화 조건:**
 * - slack.notification.enabled=false 또는 설정 없음
 */
@Component
@ConditionalOnProperty(
    prefix = "slack.notification",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class NoOpSlackNotificationAdapter : SlackNotificationPort {

    private val logger = KotlinLogging.logger {}

    override fun notifyDLQEvent(sagaId: String, eventType: String, failureReason: String) {
        logger.info { "[No-Op] DLQ 이벤트: sagaId=$sagaId, type=$eventType" }
    }

    override fun notifyUnresolvedDLQ(unresolvedCount: Long, recentDLQInfo: String) {
        logger.info { "[No-Op] 미해결 DLQ: count=$unresolvedCount" }
    }

    override fun notifySagaFailure(sagaId: String, errorMessage: String) {
        logger.info { "[No-Op] Saga 실패: sagaId=$sagaId" }
    }

    override fun notifyError(title: String, message: String) {
        logger.info { "[No-Op] 에러 알림: $title" }
    }
}
