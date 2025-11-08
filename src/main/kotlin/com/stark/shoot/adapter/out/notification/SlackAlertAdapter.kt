package com.stark.shoot.adapter.out.notification

import com.stark.shoot.application.port.out.AlertNotificationPort
import com.stark.shoot.application.port.out.CriticalAlert
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

/**
 * Slack Webhook을 통한 알림 발송 Adapter
 *
 * application.yml에서 설정:
 * ```yaml
 * notification:
 *   slack:
 *     enabled: true
 *     webhook-url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
 *     channel:
 *       critical: "#ops-critical"
 * ```
 */
@Adapter
@ConditionalOnProperty(
    prefix = "notification.slack",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class SlackAlertAdapter(
    private val restTemplate: RestTemplate = RestTemplate(),
    @Value("\${notification.slack.webhook-url:}")
    private val webhookUrl: String
) : AlertNotificationPort {

    private val logger = KotlinLogging.logger {}

    override fun sendCriticalAlert(alert: CriticalAlert) {
        if (webhookUrl.isBlank()) {
            logger.warn { "Slack webhook URL not configured, skipping alert: ${alert.title}" }
            return
        }

        try {
            val slackMessage = buildSlackMessage(alert)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val request = HttpEntity(slackMessage, headers)

            restTemplate.postForEntity(webhookUrl, request, String::class.java)

            logger.info { "Slack alert sent successfully: ${alert.title}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send Slack alert: ${alert.title}" }
        }
    }

    private fun buildSlackMessage(alert: CriticalAlert): Map<String, Any> {
        val emoji = when (alert.level) {
            com.stark.shoot.application.port.out.AlertLevel.CRITICAL -> ":rotating_light:"
            com.stark.shoot.application.port.out.AlertLevel.ERROR -> ":x:"
            com.stark.shoot.application.port.out.AlertLevel.WARNING -> ":warning:"
        }

        val color = when (alert.level) {
            com.stark.shoot.application.port.out.AlertLevel.CRITICAL -> "danger"
            com.stark.shoot.application.port.out.AlertLevel.ERROR -> "warning"
            com.stark.shoot.application.port.out.AlertLevel.WARNING -> "#ffcc00"
        }

        return mapOf(
            "channel" to alert.channel,
            "username" to "Shoot Alert Bot",
            "icon_emoji" to emoji,
            "attachments" to listOf(
                mapOf(
                    "color" to color,
                    "title" to alert.title,
                    "text" to alert.message,
                    "fields" to alert.metadata.map { (key, value) ->
                        mapOf(
                            "title" to key,
                            "value" to value,
                            "short" to true
                        )
                    },
                    "footer" to "Shoot Backend",
                    "ts" to (System.currentTimeMillis() / 1000)
                )
            )
        )
    }
}

/**
 * Slack이 비활성화되어 있을 때 사용하는 No-op Adapter
 * 로그만 출력합니다.
 */
@Adapter
@ConditionalOnProperty(
    prefix = "notification.slack",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class NoOpAlertAdapter : AlertNotificationPort {
    private val logger = KotlinLogging.logger {}

    override fun sendCriticalAlert(alert: CriticalAlert) {
        logger.warn {
            "Slack disabled - Alert would be sent: [${alert.level}] ${alert.title}\n" +
                    "${alert.message}\n" +
                    "Metadata: ${alert.metadata}"
        }
    }
}
