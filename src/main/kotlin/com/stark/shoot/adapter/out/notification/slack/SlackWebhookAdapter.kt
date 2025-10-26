package com.stark.shoot.adapter.out.notification.slack

import com.stark.shoot.application.port.out.notification.SlackNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Slack Webhook Adapter
 *
 * Slack Incoming Webhook을 통해 알림을 전송합니다.
 *
 * **설정 방법:**
 * 1. Slack Workspace에서 Incoming Webhook 생성
 * 2. application.yml에 webhook URL 설정
 * 3. slack.notification.enabled=true 설정
 *
 * **application.yml 예시:**
 * ```yaml
 * slack:
 *   notification:
 *     enabled: true
 *     webhook-url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
 *     channel: "#alerts"
 *     username: "Shoot Alert Bot"
 * ```
 */
@Component
@ConditionalOnProperty(
    prefix = "slack.notification",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class SlackWebhookAdapter(
    @Value("\${slack.notification.webhook-url:}") private val webhookUrl: String,
    @Value("\${slack.notification.channel:#alerts}") private val channel: String,
    @Value("\${slack.notification.username:Shoot Alert Bot}") private val username: String
) : SlackNotificationPort {

    private val logger = KotlinLogging.logger {}
    private val restTemplate = RestTemplate()

    override fun notifyDLQEvent(sagaId: String, eventType: String, failureReason: String) {
        val message = """
            🚨 *DLQ 이벤트 발생*

            • Saga ID: `$sagaId`
            • 이벤트 타입: `$eventType`
            • 실패 원인: $failureReason
            • 시간: ${java.time.Instant.now()}

            ⚠️ 관리자 확인이 필요합니다.
        """.trimIndent()

        sendMessage(message, ":rotating_light:")
    }

    override fun notifyUnresolvedDLQ(unresolvedCount: Long, recentDLQInfo: String) {
        val message = """
            ⚠️ *미해결 DLQ 알림*

            • 미해결 개수: *${unresolvedCount}*개

            *최근 DLQ:*
            ```
            $recentDLQInfo
            ```

            👉 확인: /api/admin/outbox-dlq
        """.trimIndent()

        sendMessage(message, ":warning:")
    }

    override fun notifySagaFailure(sagaId: String, errorMessage: String) {
        val message = """
            ❌ *Saga 실패*

            • Saga ID: `$sagaId`
            • 에러: $errorMessage
            • 시간: ${java.time.Instant.now()}
        """.trimIndent()

        sendMessage(message, ":x:")
    }

    override fun notifyError(title: String, message: String) {
        val slackMessage = """
            🔥 *$title*

            $message

            • 시간: ${java.time.Instant.now()}
        """.trimIndent()

        sendMessage(slackMessage, ":fire:")
    }

    /**
     * Slack 메시지 전송
     */
    private fun sendMessage(text: String, icon: String) {
        if (webhookUrl.isBlank()) {
            logger.warn { "Slack Webhook URL이 설정되지 않았습니다. 알림을 건너뜁니다." }
            return
        }

        try {
            val payload = SlackMessage(
                text = text,
                channel = channel,
                username = username,
                icon_emoji = icon
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val request = HttpEntity(payload, headers)
            val response = restTemplate.postForEntity(webhookUrl, request, String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                logger.debug { "Slack 알림 전송 성공" }
            } else {
                logger.error { "Slack 알림 전송 실패: ${response.statusCode}" }
            }

        } catch (e: Exception) {
            logger.error(e) { "Slack 알림 전송 중 예외 발생" }
        }
    }
}

/**
 * Slack 메시지 페이로드
 */
data class SlackMessage(
    val text: String,
    val channel: String? = null,
    val username: String? = null,
    val icon_emoji: String? = null
)
