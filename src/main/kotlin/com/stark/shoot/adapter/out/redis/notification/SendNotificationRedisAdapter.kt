package com.stark.shoot.adapter.out.redis.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.redis.core.RedisTemplate

@Adapter
class SendNotificationRedisAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : SendNotificationPort {

    companion object {
        private const val NOTIFICATION_CHANNEL_PREFIX = "notification:"
        private const val NOTIFICATION_BROADCAST_CHANNEL = "notification:broadcast"
    }


    /**
     * 알림 전송
     *
     * @param notification 알림 객체
     * @return 성공 여부
     */
    override fun sendNotification(notification: Notification): Boolean {
        try {
            // Convert the notification to a JSON string
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Publish the notification to the user's channel
            val userChannel = "$NOTIFICATION_CHANNEL_PREFIX${notification.userId}"
            redisTemplate.convertAndSend(userChannel, notificationJson)

            return true
        } catch (e: Exception) {
            // Log the error
            println("Error sending notification: ${e.message}")
            return false
        }
    }

    /**
     * 알림 목록 전송
     *
     * @param notifications 알림 객체 목록
     * @return 성공한 알림 개수
     */
    override fun sendNotifications(notifications: List<Notification>): Int {
        return notifications.count { sendNotification(it) }
    }

    /**
     * 알림을 브로드캐스트합니다.
     *
     * @param notification 알림 객체
     * @return 성공 여부
     */
    fun broadcastNotification(notification: Notification): Boolean {
        try {
            // Convert the notification to a JSON string
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Publish the notification to the broadcast channel
            redisTemplate.convertAndSend(NOTIFICATION_BROADCAST_CHANNEL, notificationJson)

            return true
        } catch (e: Exception) {
            // Log the error
            println("Error broadcasting notification: ${e.message}")
            return false
        }
    }

}