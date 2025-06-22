package com.stark.shoot.adapter.out.redis.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.RedisOperationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate

@Adapter
class SendNotificationRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : SendNotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_CHANNEL_PREFIX = "notification:user:"
    }

    /**
     * 알림을 Redis를 통해 전송합니다.
     * 사용자별 채널에 알림을 발행하여 실시간으로 전달합니다.
     *
     * @param notification 알림 객체
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    override fun sendNotification(notification: Notification) {
        try {
            val channel = "$NOTIFICATION_CHANNEL_PREFIX${notification.userId.value}"
            val notificationJson = objectMapper.writeValueAsString(notification)
            
            // Redis pub/sub 채널에 알림 발행
            val result = redisTemplate.convertAndSend(channel, notificationJson)
            
            logger.info { "알림이 Redis 채널에 발행되었습니다: userId=${notification.userId.value}, type=${notification.type}, result=$result" }
        } catch (e: Exception) {
            val errorMessage = "Redis를 통한 알림 전송 중 오류가 발생했습니다: ${e.message}"
            logger.error(e) { errorMessage }
            throw RedisOperationException(errorMessage, e)
        }
    }

    /**
     * 여러 알림을 Redis를 통해 전송합니다.
     * 각 사용자별 채널에 알림을 발행하여 실시간으로 전달합니다.
     *
     * @param notifications 알림 객체 목록
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    override fun sendNotifications(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            logger.info { "전송할 알림이 없습니다." }
            return
        }
        
        try {
            // 사용자별로 알림 그룹화
            val notificationsByUser = notifications.groupBy { it.userId }
            
            // 각 사용자별로 알림 전송
            notificationsByUser.forEach { (userId, userNotifications) ->
                val channel = "$NOTIFICATION_CHANNEL_PREFIX${userId.value}"
                
                // 각 알림을 개별적으로 발행
                userNotifications.forEach { notification ->
                    val notificationJson = objectMapper.writeValueAsString(notification)
                    redisTemplate.convertAndSend(channel, notificationJson)
                }
                
                logger.info { "사용자($userId)에게 ${userNotifications.size}개의 알림이 Redis 채널에 발행되었습니다." }
            }
        } catch (e: Exception) {
            val errorMessage = "Redis를 통한 다중 알림 전송 중 오류가 발생했습니다: ${e.message}"
            logger.error(e) { errorMessage }
            throw RedisOperationException(errorMessage, e)
        }
    }
}