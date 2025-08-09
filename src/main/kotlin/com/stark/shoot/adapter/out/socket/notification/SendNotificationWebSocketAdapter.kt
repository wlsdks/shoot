package com.stark.shoot.adapter.out.socket.notification

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging

@Adapter
class SendNotificationWebSocketAdapter(
    private val webSocketMessageBroker: WebSocketMessageBroker
) : SendNotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_DESTINATION_PREFIX = "/topic/notifications/"
    }

    override fun sendNotification(notification: Notification) {
        val destination = "$NOTIFICATION_DESTINATION_PREFIX${notification.userId.value}"
        webSocketMessageBroker.sendMessage(destination, notification)
            .exceptionally { ex ->
                logger.error(ex) { "WebSocket 알림 전송 실패: userId=${notification.userId.value}, type=${notification.type}" }
                false
            }
    }

    override fun sendNotifications(notifications: List<Notification>) {
        notifications.forEach { sendNotification(it) }
    }
}

