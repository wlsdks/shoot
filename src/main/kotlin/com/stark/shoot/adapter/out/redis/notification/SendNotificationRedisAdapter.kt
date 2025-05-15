package com.stark.shoot.adapter.out.redis.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.ConcurrentHashMap

@Adapter
class SendNotificationRedisAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : SendNotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_CHANNEL_PREFIX = "notification:"
        private const val NOTIFICATION_BROADCAST_CHANNEL = "notification:broadcast"
        private const val BATCH_SIZE = 10 // 배치 처리 크기
    }

    /**
     * 알림 전송
     *
     * @param notification 알림 객체
     * @return 성공 여부
     */
    override fun sendNotification(notification: Notification): Boolean {
        try {
            // JSON 문자열로 변환
            val notificationJson = objectMapper.writeValueAsString(notification)
            logger.debug { "Sending notification to user ${notification.userId}: $notificationJson" }

            // Redis 채널에 알림 전송
            val userChannel = "$NOTIFICATION_CHANNEL_PREFIX${notification.userId}"
            val receiverCount = redisTemplate.convertAndSend(userChannel, notificationJson)

            logger.debug { "Notification sent to user ${notification.userId}, received by $receiverCount subscribers" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Error sending notification to user ${notification.userId}: ${notification.id}" }
            return false
        }
    }

    /**
     * 알림 목록 전송 - 배치 처리 최적화
     *
     * @param notifications 알림 객체 목록
     * @return 성공한 알림 개수
     */
    override fun sendNotifications(notifications: List<Notification>): Int {
        if (notifications.isEmpty()) {
            return 0
        }

        logger.debug { "Sending ${notifications.size} notifications" }

        // 사용자별로 알림 그룹화
        val notificationsByUser = notifications.groupBy { it.userId }
        val successCount = ConcurrentHashMap<Long, Int>()

        // 각 사용자별로 알림 처리
        notificationsByUser.forEach { (userId, userNotifications) ->
            // 배치 단위로 처리
            userNotifications.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    val userChannel = "$NOTIFICATION_CHANNEL_PREFIX$userId"
                    val batchJson = objectMapper.writeValueAsString(batch)

                    val receiverCount = redisTemplate.convertAndSend(userChannel, batchJson)
                    logger.debug { "Batch of ${batch.size} notifications sent to user $userId, received by $receiverCount subscribers" }

                    // 성공 카운트 증가
                    successCount.compute(userId) { _, count -> (count ?: 0) + batch.size }
                } catch (e: Exception) {
                    logger.error(e) { "Error sending batch of notifications to user $userId" }
                }
            }
        }

        val totalSuccess = successCount.values.sum()
        logger.debug { "Successfully sent $totalSuccess out of ${notifications.size} notifications" }
        return totalSuccess
    }

    /**
     * 알림을 브로드캐스트합니다.
     *
     * @param notification 알림 객체
     * @return 성공 여부
     */
    fun broadcastNotification(notification: Notification): Boolean {
        try {
            // JSON 문자열로 변환
            val notificationJson = objectMapper.writeValueAsString(notification)
            logger.debug { "Broadcasting notification: $notificationJson" }

            // Redis 채널에 알림 전송
            val receiverCount = redisTemplate.convertAndSend(NOTIFICATION_BROADCAST_CHANNEL, notificationJson)

            logger.debug { "Notification broadcast, received by $receiverCount subscribers" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Error broadcasting notification: ${notification.id}" }
            return false
        }
    }

}
