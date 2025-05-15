package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.out.notification.DeleteNotificationPort
import com.stark.shoot.application.port.out.notification.LoadNotificationPort
import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class NotificationManagementService(
    private val loadNotificationPort: LoadNotificationPort,
    private val saveNotificationPort: SaveNotificationPort,
    private val deleteNotificationPort: DeleteNotificationPort
) : NotificationManagementUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 객체
     * @throws IllegalArgumentException 알림이 존재하지 않거나 해당 유저의 것이 아닌 경우
     */
    override fun markAsRead(
        notificationId: String,
        userId: Long
    ): Notification {
        try {
            val notification = loadNotificationPort.loadNotificationById(notificationId)
                ?: run {
                    throw IllegalArgumentException("Notification not found with ID: $notificationId")
                }

            // 유저 ID와 알림 ID를 비교하여 알림이 해당 유저의 것인지 확인합니다.
            if (notification.userId != userId) {
                throw IllegalArgumentException("Notification does not belong to user with ID: $userId")
            }

            // 이미 읽은 알림인 경우 바로 반환
            if (notification.isRead) {
                return notification
            }

            // 읽음 처리합니다.
            val updatedNotification = notification.markAsRead()

            // 저장합니다.
            val savedNotification = saveNotificationPort.saveNotification(updatedNotification)

            return savedNotification
        } catch (e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw e
        }
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsRead(userId: Long): Int {
        try {
            val notifications = loadNotificationPort.loadUnreadNotificationsForUser(userId, Int.MAX_VALUE, 0)

            if (notifications.isEmpty()) {
                return 0
            }

            // 모든 읽지 않은 알림을 읽음 처리합니다.
            val updatedNotifications = notifications.map { it.markAsRead() }

            // 저장합니다.
            saveNotificationPort.saveNotifications(updatedNotifications)

            return updatedNotifications.size
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 모든 알림을 타입별로 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadByType(
        userId: Long,
        type: NotificationType
    ): Int {
        try {
            val notifications = loadNotificationPort.loadNotificationsByType(userId, type, Int.MAX_VALUE, 0)
                .filter { !it.isRead }

            if (notifications.isEmpty()) {
                return 0
            }

            // 모든 읽지 않은 알림을 읽음 처리합니다.
            val updatedNotifications = notifications.map { it.markAsRead() }

            // 저장합니다.
            saveNotificationPort.saveNotifications(updatedNotifications)

            return updatedNotifications.size
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 모든 알림을 소스별로 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (optional)
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadBySource(
        userId: Long,
        sourceType: SourceType,
        sourceId: String?
    ): Int {
        try {
            val notifications =
                loadNotificationPort.loadNotificationsBySource(userId, sourceType, sourceId, Int.MAX_VALUE, 0)
                    .filter { !it.isRead }

            if (notifications.isEmpty()) {
                return 0
            }

            // 모든 읽지 않은 알림을 읽음 처리합니다.
            val updatedNotifications = notifications.map { it.markAsRead() }

            // 저장합니다.
            saveNotificationPort.saveNotifications(updatedNotifications)

            return updatedNotifications.size
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 삭제 성공 여부
     * @throws ResourceNotFoundException 알림을 찾을 수 없거나 해당 사용자의 알림이 아닌 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotification(
        notificationId: String,
        userId: Long
    ): Boolean {
        val notification = loadNotificationPort.loadNotificationById(notificationId)
            ?: throw ResourceNotFoundException("알림을 찾을 수 없습니다: $notificationId")

        // 유저 ID와 알림 ID를 비교하여 알림이 해당 유저의 것인지 확인합니다.
        if (notification.userId != userId) {
            throw ResourceNotFoundException("해당 사용자의 알림이 아닙니다: $notificationId")
        }

        // 알림을 삭제합니다.
        deleteNotificationPort.deleteNotification(notificationId)

        return true
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteAllNotifications(userId: Long): Int {
        // 사용자의 모든 알림을 삭제합니다.
        return deleteNotificationPort.deleteAllNotificationsForUser(userId)
    }

}
