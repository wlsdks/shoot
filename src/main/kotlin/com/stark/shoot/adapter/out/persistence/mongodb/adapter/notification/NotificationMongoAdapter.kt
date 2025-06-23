package com.stark.shoot.adapter.out.persistence.mongodb.adapter.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.mongodb.document.notification.NotificationDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.NotificationMongoRepository
import com.stark.shoot.application.port.out.notification.NotificationPort
import com.stark.shoot.domain.event.NotificationEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.RedisOperationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate

@Adapter
class NotificationMongoAdapter(
    private val notificationMongoRepository: NotificationMongoRepository,
    private val mongoTemplate: MongoTemplate,
    private val redisTemplate: StringRedisTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : NotificationPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val NOTIFICATION_CHANNEL_PREFIX = "notification:user:"
        private const val NOTIFICATION_EVENTS_TOPIC = "notification-events"
    }

    override fun saveNotification(notification: Notification): Notification {
        val document = NotificationDocument.fromDomain(notification)
        val saved = notificationMongoRepository.save(document)
        return saved.toDomain()
    }

    override fun saveNotifications(notifications: List<Notification>): List<Notification> {
        val docs = notifications.map { NotificationDocument.fromDomain(it) }
        val saved = notificationMongoRepository.saveAll(docs)
        return saved.map { it.toDomain() }
    }

    override fun deleteNotification(notificationId: NotificationId) {
        try {
            if (notificationMongoRepository.existsById(notificationId.value)) {
                notificationMongoRepository.deleteById(notificationId.value)
            } else {
                throw MongoOperationException("알림을 찾을 수 없습니다: $notificationId")
            }
        } catch (e: Exception) {
            throw MongoOperationException("알림 삭제 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    override fun deleteAllNotificationsForUser(userId: UserId): Int {
        return notificationMongoRepository.deleteByUserId(userId.value)
    }

    override fun deleteNotificationsByType(userId: UserId, type: String): Int {
        val query = Query.query(
            Criteria.where("userId").isEqualTo(userId.value).and("type").isEqualTo(type)
        )
        val result = mongoTemplate.remove(query, "notifications")
        return result.deletedCount.toInt()
    }

    override fun deleteNotificationsBySource(userId: UserId, sourceType: String, sourceId: String?): Int {
        val criteria = Criteria.where("userId").isEqualTo(userId.value)
            .and("sourceType").isEqualTo(sourceType)
        if (sourceId != null) {
            criteria.and("sourceId").isEqualTo(sourceId)
        }
        val query = Query.query(criteria)
        val result = mongoTemplate.remove(query, "notifications")
        return result.deletedCount.toInt()
    }

    override fun loadNotificationById(id: NotificationId): Notification? {
        return notificationMongoRepository.findById(id.value).map { it.toDomain() }.orElse(null)
    }

    override fun loadNotificationsForUser(userId: UserId, limit: Int, offset: Int): List<Notification> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return notificationMongoRepository.findByUserId(userId.value, pageable).map { it.toDomain() }
    }

    override fun loadUnreadNotificationsForUser(userId: UserId, limit: Int, offset: Int): List<Notification> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return notificationMongoRepository.findByUserIdAndIsReadFalse(userId.value, pageable)
            .map { it.toDomain() }
    }

    override fun loadNotificationsByType(
        userId: UserId,
        type: NotificationType,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return notificationMongoRepository.findByUserIdAndType(userId.value, type.name, pageable)
            .map { it.toDomain() }
    }

    override fun loadNotificationsBySource(
        userId: UserId,
        sourceType: SourceType,
        sourceId: String?,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return if (sourceId != null) {
            notificationMongoRepository.findByUserIdAndSourceTypeAndSourceId(
                userId.value,
                sourceType.name,
                sourceId,
                pageable
            )
                .map { it.toDomain() }
        } else {
            notificationMongoRepository.findByUserIdAndSourceType(userId.value, sourceType.name, pageable)
                .map { it.toDomain() }
        }
    }

    override fun countUnreadNotifications(userId: UserId): Int {
        return notificationMongoRepository.countByUserIdAndIsReadFalse(userId.value)
    }

    override fun sendNotification(notification: Notification) {
        try {
            val channel = "$NOTIFICATION_CHANNEL_PREFIX${notification.userId.value}"
            val json = objectMapper.writeValueAsString(notification)
            redisTemplate.convertAndSend(channel, json)
        } catch (e: Exception) {
            logger.error(e) { "Redis를 통한 알림 전송 중 오류가 발생했습니다" }
            throw RedisOperationException("Redis 전송 실패", e)
        }
    }

    override fun sendNotifications(notifications: List<Notification>) {
        notifications.forEach { sendNotification(it) }
    }

    override fun publishEvent(event: NotificationEvent) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event.sourceId, eventJson)
        } catch (e: Exception) {
            logger.error(e) { "Kafka 이벤트 발행 실패" }
            throw KafkaPublishException("Kafka 이벤트 발행 실패", e)
        }
    }
}
