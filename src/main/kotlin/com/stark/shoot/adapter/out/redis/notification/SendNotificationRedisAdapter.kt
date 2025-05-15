package com.stark.shoot.adapter.out.redis.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.RedisOperationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate

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
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    override fun sendNotification(notification: Notification) {
        try {
            // JSON 문자열로 변환
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Redis 채널에 알림 전송
            val userChannel = "$NOTIFICATION_CHANNEL_PREFIX${notification.userId}"
            redisTemplate.convertAndSend(userChannel, notificationJson)
        } catch (e: Exception) {
            throw RedisOperationException("Failed to send notification: ${e.message}", e)
        }
    }

    /**
     * 알림 목록 전송 - 배치 처리 최적화
     *
     * @param notifications 알림 객체 목록
     * @throws RedisOperationException 모든 알림 전송 실패 시 발생
     */
    override fun sendNotifications(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            return
        }

        // 사용자별로 알림 그룹화
        val notificationsByUser = notifications.groupBy { it.userId }
        val failedBatches = mutableListOf<String>()

        // 각 사용자별로 알림 처리
        notificationsByUser.forEach { (userId, userNotifications) ->
            // 배치 단위로 처리
            userNotifications.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    val userChannel = "$NOTIFICATION_CHANNEL_PREFIX$userId"
                    val batchJson = objectMapper.writeValueAsString(batch)

                    redisTemplate.convertAndSend(userChannel, batchJson)
                } catch (e: Exception) {
                    failedBatches.add("User $userId, batch size ${batch.size}")
                    logger.error(e) { "Failed to send notification batch to user $userId" }
                }
            }
        }

        // 모든 배치가 실패한 경우에만 예외 발생
        if (failedBatches.size == notificationsByUser.values.sumOf { it.chunked(BATCH_SIZE).size }) {
            throw RedisOperationException("Failed to send all notification batches")
        }
    }

    /**
     * 알림을 브로드캐스트합니다.
     *
     * @param notification 알림 객체
     * @throws RedisOperationException 알림 브로드캐스트 실패 시 발생
     */
    fun broadcastNotification(notification: Notification) {
        try {
            // JSON 문자열로 변환
            val notificationJson = objectMapper.writeValueAsString(notification)

            // Redis 채널에 알림 전송
            redisTemplate.convertAndSend(NOTIFICATION_BROADCAST_CHANNEL, notificationJson)
        } catch (e: Exception) {
            throw RedisOperationException("Failed to broadcast notification: ${e.message}", e)
        }
    }

}
