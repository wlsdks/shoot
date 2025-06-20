package com.stark.shoot.adapter.out.persistence.mongodb.adapter.notification

import com.stark.shoot.adapter.out.persistence.mongodb.repository.NotificationMongoRepository
import com.stark.shoot.application.port.out.notification.DeleteNotificationPort
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@Adapter
class DeleteNotificationMongoAdapter(
    private val notificationMongoRepository: NotificationMongoRepository,
    private val mongoTemplate: MongoTemplate
) : DeleteNotificationPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @throws ResourceNotFoundException 알림을 찾을 수 없는 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotification(notificationId: NotificationId) {
        try {
            if (notificationMongoRepository.existsById(notificationId.value)) {
                notificationMongoRepository.deleteById(notificationId.value)
            } else {
                throw ResourceNotFoundException("알림을 찾을 수 없습니다: $notificationId")
            }
        } catch (e: ResourceNotFoundException) {
            throw e
        } catch (e: Exception) {
            throw MongoOperationException("알림 삭제 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    /**
     * 사용자의 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteAllNotificationsForUser(userId: UserId): Int {
        try {
            val query = Query(Criteria.where("userId").isEqualTo(userId.value))
            val count = mongoTemplate.count(query, "notifications")
            val result = notificationMongoRepository.deleteByUserId(userId.value)

            return result
        } catch (e: Exception) {
            throw MongoOperationException("사용자 알림 삭제 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    /**
     * 사용자의 특정 타입의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotificationsByType(
        userId: UserId,
        type: String
    ): Int {
        try {
            val query = Query.query(
                Criteria.where("userId").isEqualTo(userId.value)
                    .and("type").isEqualTo(type)
            )

            val count = mongoTemplate.count(query, "notifications")
            val result = mongoTemplate.remove(query, "notifications")

            return result.deletedCount.toInt()
        } catch (e: Exception) {
            throw MongoOperationException("타입별 알림 삭제 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    /**
     * 사용자의 특정 소스의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (null일 경우 모든 소스 ID에 대해 삭제)
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotificationsBySource(
        userId: UserId,
        sourceType: String,
        sourceId: String?
    ): Int {
        try {
            val criteria = Criteria.where("userId").isEqualTo(userId.value)
                .and("sourceType").isEqualTo(sourceType)

            if (sourceId != null) {
                criteria.and("sourceId").isEqualTo(sourceId)
            }

            val query = Query.query(criteria)
            val count = mongoTemplate.count(query, "notifications")
            val result = mongoTemplate.remove(query, "notifications")

            return result.deletedCount.toInt()
        } catch (e: Exception) {
            throw MongoOperationException("소스별 알림 삭제 중 오류가 발생했습니다: ${e.message}", e)
        }
    }
}
