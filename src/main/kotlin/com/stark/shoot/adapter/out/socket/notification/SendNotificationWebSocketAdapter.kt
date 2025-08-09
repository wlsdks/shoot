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
        private const val USER_DESTINATION = "/queue/notifications"
    }

    /**
     * 알림을 WebSocket을 통해 전송합니다.
     * 사용자별로 WebSocket 메시지를 전송하여 실시간으로 알림을 전달합니다.
     *
     * @param notification 알림 객체
     */
    override fun sendNotification(notification: Notification) {
        try {
            webSocketMessageBroker.sendToUser(
                notification.userId.value.toString(),
                USER_DESTINATION,
                notification
            ).exceptionally { ex ->
                logger.error(ex) { "WebSocket 알림 전송 실패: userId=${notification.userId.value}, type=${notification.type}" }
                false
            }
        } catch (ex: Exception) {
            logger.error(ex) { "WebSocket 알림 전송 중 오류 발생: userId=${notification.userId.value}, type=${notification.type}" }
        }
    }

    /**
     * 여러 알림을 WebSocket을 통해 전송합니다.
     * 각 알림은 사용자별로 전송됩니다.
     *
     * @param notifications 알림 객체 목록
     */
    override fun sendNotifications(notifications: List<Notification>) {
        notifications.forEach { sendNotification(it) }
    }

}
